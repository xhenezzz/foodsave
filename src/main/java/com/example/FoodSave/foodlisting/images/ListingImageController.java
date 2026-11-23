package com.example.FoodSave.foodlisting.images;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
@RequiredArgsConstructor
public class ListingImageController {
    private final ListingImageService service;

    @GetMapping
    public List<ListingImageResponse> list(@PathVariable UUID listingId) {
        return service.list(listingId);
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID listingId, @PathVariable UUID imageId) {
        var result = service.content(listingId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(result.bytes());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingImageResponse> upload(@PathVariable UUID listingId,
                                                       @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(listingId, file));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID listingId, @PathVariable UUID imageId) {
        service.remove(listingId, imageId);
        return ResponseEntity.noContent().build();
    }
}
