package com.deappie.movies.movie;

import org.jspecify.annotations.Nullable;

record MovieFilter(@Nullable String q, @Nullable Long genreId) {}
