package com.example.FoodSave.foodlisting.images;

import com.example.FoodSave.auth.cfg.CurrentUser;
import com.example.FoodSave.auth.cfg.UserPrincipal;
import com.example.FoodSave.auth.entity.Role;
import com.example.FoodSave.auth.repo.UserRepository;
import com.example.FoodSave.foodlisting.entity.FoodListing;
import com.example.FoodSave.foodlisting.repo.FoodListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ListingImageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMAGES = 5;
    private static final int MAX_DIMENSION = 7000;
    private final ListingImageRepository images;
    private final FoodListingRepository listings;
    private final UserRepository users;
    @Value("${foodsave.uploads.path:./uploads/foodsave}")
    private String uploadPath;

    public List<ListingImageResponse> list(UUID listingId) {
        if (!listings.existsById(listingId)) throw error(HttpStatus.NOT_FOUND, "Объявление не найдено");
        return images.findByListingIdOrderBySortOrderAscCreatedAtAsc(listingId).stream()
                .map(ListingImageResponse::from).toList();
    }

    @Transactional
    public ListingImageResponse upload(UUID listingId, MultipartFile file) {
        FoodListing listing = ownedListing(listingId);
        if (images.countByListingId(listingId) >= MAX_IMAGES)
            throw error(HttpStatus.BAD_REQUEST, "Можно загрузить максимум 5 фотографий");
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES)
            throw error(HttpStatus.BAD_REQUEST, "Фото должно быть от 1 байта до 5 МБ");

        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException ex) { throw error(HttpStatus.BAD_REQUEST, "Не удалось прочитать фотографию"); }
        if (bytes.length > MAX_BYTES) throw error(HttpStatus.BAD_REQUEST, "Максимальный размер 5 МБ");
        String mime = inspect(bytes);
        String extension = mime.equals("image/jpeg") ? ".jpg" : ".png";
        String filename = UUID.randomUUID() + extension;
        Path path = directory().resolve(filename);
        try { Files.write(path, bytes, StandardOpenOption.CREATE_NEW); }
        catch (IOException ex) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка сохранения фотографии"); }
        try {
            ListingImage img = new ListingImage();
            img.setListing(listing);
            img.setStoredFilename(filename);
            img.setContentType(mime);
            img.setSortOrder((int) images.countByListingId(listingId));
            img.setCreatedAt(LocalDateTime.now());
            return ListingImageResponse.from(images.saveAndFlush(img));
        } catch (RuntimeException ex) {
            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            throw ex;
        }
    }

    public ImageBytes content(UUID listingId, UUID imageId) {
        ListingImage img = images.findByIdAndListingId(imageId, listingId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Фото не найдено"));
        try { return new ImageBytes(Files.readAllBytes(directory().resolve(img.getStoredFilename())), img.getContentType()); }
        catch (IOException ex) { throw error(HttpStatus.NOT_FOUND, "Файл фотографии не найден"); }
    }

    @Transactional
    public void remove(UUID listingId, UUID imageId) {
        ownedListing(listingId);
        ListingImage img = images.findByIdAndListingId(imageId, listingId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Фото не найдено"));
        images.delete(img);
        images.flush();
        try { Files.deleteIfExists(directory().resolve(img.getStoredFilename())); }
        catch (IOException ex) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось удалить фотографию"); }
    }

    private FoodListing ownedListing(UUID listingId) {
        UserPrincipal principal = CurrentUser.get();
        var user = users.findById(principal.getId())
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
        if (user.getRole() != Role.BUSINESS) throw error(HttpStatus.FORBIDDEN, "Только бизнес может загружать фото");
        FoodListing listing = listings.findById(listingId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Объявление не найдено"));
        if (!listing.getBusiness().getUser().getId().equals(user.getId()))
            throw error(HttpStatus.FORBIDDEN, "Это не ваше объявление");
        return listing;
    }

    private Path directory() {
        try { Path dir = Paths.get(uploadPath).toAbsolutePath().normalize(); Files.createDirectories(dir); return dir; }
        catch (IOException ex) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "Папка изображений недоступна"); }
    }

    // Checks actual bytes and dimensions, not the untrusted file extension or Content-Type header.
    private String inspect(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("Invalid image stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png"))
                    throw new IOException("Unsupported image format");
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_DIMENSION || h > MAX_DIMENSION || ((long)w * h) > 25_000_000)
                    throw new IOException("Image dimensions too large");
                if (reader.read(0) == null) throw new IOException("Image decoding failed");
                return format.equals("png") ? "image/png" : "image/jpeg";
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException ex) {
            throw error(HttpStatus.BAD_REQUEST, "Допускаются только корректные JPG/PNG до 5 МБ (до 25 Мп)");
        }
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
    public record ImageBytes(byte[] bytes, String contentType) {}
}
