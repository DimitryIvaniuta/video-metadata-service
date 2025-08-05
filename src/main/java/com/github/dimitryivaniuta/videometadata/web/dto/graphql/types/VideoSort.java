package com.github.dimitryivaniuta.videometadata.web.dto.graphql.types;

/**
 * Sort keys for video listings.
 */
public enum VideoSort {
    IMPORTED_AT,   // maps to DB column "created_at"
    UPLOAD_DATE,   // maps to "upload_date"
    TITLE          // maps to "title"
}