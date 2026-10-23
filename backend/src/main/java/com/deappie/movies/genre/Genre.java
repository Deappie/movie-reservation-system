package com.deappie.movies.genre;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("genres")
record Genre(@Id Long id, String name) {}
