package cop5556fa20.runtime;

import cop5556fa20.Scanner;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PLPImageUtils {
    public static final  String className = "cop5556fa20/runtime/PLPImageUtils";
    public static final String desc = "Lcop5556fa20/runtime/PLPImageUtils;";

    private static final String INT_DESC = "I";
    private static final String INTEGER_DESC = "Ljava/lang/Integer;";
    private static final String OBJECT_DESC = "Ljava/lang/Object;";

    public static String createImageSig =
            "(" + Scanner.KIND_DESC + INTEGER_DESC + INTEGER_DESC + OBJECT_DESC + INT_DESC + INT_DESC + ")"
            + PLPImage.desc;

    public static PLPImage createImage(Scanner.Kind op, Integer width, Integer height, Object source, int line, int posLine) throws Exception {
        Dimension dimension = null;
        BufferedImage bufferedImage = null;

        if (width != null) {
            dimension = new Dimension(width, height);
        }

        if (op == Scanner.Kind.LARROW) {
            if (source instanceof String) {
                bufferedImage = BufferedImageUtils.fetchBufferedImage((String) source);
            } else if (source instanceof PLPImage) {
                bufferedImage = BufferedImageUtils.copyBufferedImage(((PLPImage) source).image);
            }
            if (dimension != null) {
                bufferedImage = BufferedImageUtils.resizeBufferedImage(bufferedImage, width, height);
            }
        }

        if (op == Scanner.Kind.ASSIGN) {
            PLPImage sourceImage = ((PLPImage) source);
            if (sourceImage == null || sourceImage.image == null) {
                throw new PLPImage.PLPImageException(line, posLine, "RHS image is null");
            }
            if (dimension != null) {
                int sourceWidth = sourceImage.getWidthThrows(line, posLine);
                int sourceHeight = sourceImage.getHeightThrows(line, posLine);
                if (sourceWidth != width || sourceHeight != height) {
                    throw new PLPImage.PLPImageException(line, posLine, "size does not matching while assigning the image");
                }
            }
            bufferedImage = sourceImage.image;
        }

        return new PLPImage(bufferedImage, dimension);
    }

    public static String copyImageSig = "(" + PLPImage.desc + OBJECT_DESC + ")" + PLPImage.desc;
    public static PLPImage copyImage(PLPImage plpImage, Object source) throws Exception {
        BufferedImage bufferedImage = null;
        if (source instanceof String) {
            bufferedImage = BufferedImageUtils.fetchBufferedImage((String)source);
        } else if (source instanceof PLPImage) {
            bufferedImage = BufferedImageUtils.copyBufferedImage(((PLPImage)source).image);
        }

        if (plpImage.declaredSize != null) {
            bufferedImage = BufferedImageUtils.resizeBufferedImage(bufferedImage, plpImage.declaredSize.width,
                    plpImage.declaredSize.height);
        }
        plpImage.image = bufferedImage;
        return plpImage;
    }

    public static String assignImageSig = "(" + PLPImage.desc + PLPImage.desc + INT_DESC + INT_DESC + ")" + PLPImage.desc;
    public static PLPImage assignImage(PLPImage lhsImage, PLPImage rhsImage, int line, int posLine) throws Exception {
        if (rhsImage == null || rhsImage.image == null) {
            throw new PLPImage.PLPImageException(line, posLine, "RHS image is null when assigning the image");
        }
        if (lhsImage.declaredSize != null) {
            if (lhsImage.declaredSize.width != rhsImage.getWidthThrows(line, posLine)
                    || lhsImage.declaredSize.height != rhsImage.getHeightThrows(line, posLine)) {
                throw new PLPImage.PLPImageException(line, posLine,
                        "LHS image declared size doesn't match with the RHS image size while assigning the image");
            }
        }
        lhsImage.image = rhsImage.image;
        return lhsImage;
    }
}
