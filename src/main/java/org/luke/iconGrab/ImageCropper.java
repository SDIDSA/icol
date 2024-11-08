package org.luke.iconGrab;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public class ImageCropper {
    /**
     * Crops transparent edges from a BufferedImage, returning a new image with only
     * the non-transparent content.
     *
     * @param image The source image to crop
     * @return A new BufferedImage with transparent edges removed, or the original image
     *         if no cropping is needed
     */
    public static BufferedImage cropTransparency(BufferedImage image) {
        // Find the bounds of non-transparent pixels
        Rectangle bounds = getContentBounds(image);

        // If no cropping is needed, return the original
        if (bounds.x == 0 && bounds.y == 0 &&
                bounds.width == image.getWidth() &&
                bounds.height == image.getHeight()) {
            return image;
        }

        // Create and return the cropped image
        return image.getSubimage(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height
        );
    }

    /**
     * Finds the bounds of non-transparent content in the image.
     */
    private static Rectangle getContentBounds(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int pad = width / 20;

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = pad; y < height - pad; y++) {
            for (int x = pad; x < width - pad; x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;

                if (alpha != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if(minX <= pad) {
            minX = 0;
            maxX = width - 1;
        }

        if(minY <= pad) {
            minY = 0;
            maxY = height - 1;
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle(0, 0, width, height);
        }

        int from = Math.min(minX, minY);
        int to = Math.max(maxX, maxY);

        return new Rectangle(
                from,
                from,
                to - from + 1,
                to - from + 1
        );
    }
}