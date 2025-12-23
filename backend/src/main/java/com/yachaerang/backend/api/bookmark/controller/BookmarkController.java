package com.yachaerang.backend.api.bookmark.controller;

import com.yachaerang.backend.api.bookmark.dto.response.BookmarkResponseDto;
import com.yachaerang.backend.api.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("")
    public void register(
            @RequestHeader("Authorization") String token,
            @RequestParam("articleId") Long articleId
    ) {
        bookmarkService.registerBookmark(articleId);
    }

    @DeleteMapping("")
    public void delete(
            @RequestHeader("Authorization") String token,
            @RequestParam("articleId") Long articleId
    ) {
        bookmarkService.eraseBookmark(articleId);
    }

    @GetMapping("")
    public List<BookmarkResponseDto.GetAllDto> getAll(
            @RequestHeader("Authorization") String token
    ) {
        return bookmarkService.getAll();
    }
}
