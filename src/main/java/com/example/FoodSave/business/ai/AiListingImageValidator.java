package com.example.FoodSave.business.ai;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

@Component
public class AiListingImageValidator {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;
    private static final int MAX_DIMENSION = 7_000;

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Загрузи фотографию продукта");
        }
        if (file.getSize() > MAX_BYTES) {
            throw badRequest("Фотография должна быть не больше 5 МБ");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw badRequest("Не удалось прочитать фотографию");
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw badRequest("Фотография должна быть от 1 байта до 5 МБ");
        }

        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("Invalid image stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png")) {
                    throw new IOException("Unsupported image format");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new IOException("Image dimensions too large");
                }
                if (reader.read(0) == null) throw new IOException("Image decoding failed");
                String mimeType = format.equals("png") ? "image/png" : "image/jpeg";
                return new ValidatedImage(bytes, mimeType);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            throw badRequest("Допускаются только корректные JPG/PNG до 5 МБ и 25 Мп");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record ValidatedImage(byte[] bytes, String mimeType) {}
}
