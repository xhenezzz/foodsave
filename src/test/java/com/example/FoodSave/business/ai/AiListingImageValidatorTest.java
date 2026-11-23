package com.example.FoodSave.business.ai;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AiListingImageValidatorTest {
    private final AiListingImageValidator validator = new AiListingImageValidator();

    @Test
    void acceptsRealPngBytesAndIgnoresClientMimeType() throws Exception {
        BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        var file = new MockMultipartFile("image", "product.jpg", "image/jpeg", output.toByteArray());

        var result = validator.validate(file);

        assertEquals("image/png", result.mimeType());
        assertArrayEquals(output.toByteArray(), result.bytes());
    }

    @Test
    void rejectsFakeImageAndOversizedUpload() {
        var fake = new MockMultipartFile("image", "product.png", "image/png", "not an image".getBytes());
        assertThrows(ResponseStatusException.class, () -> validator.validate(fake));

        var oversized = new MockMultipartFile("image", "large.png", "image/png",
                new byte[(int) AiListingImageValidator.MAX_BYTES + 1]);
        assertThrows(ResponseStatusException.class, () -> validator.validate(oversized));
    }
}
