package com.deappie.movies.user;

import jakarta.validation.constraints.NotNull;

record ChangeRoleRequest(@NotNull Role role) {}
