-- Reference data the app needs to be usable: genres, halls and their seats.

INSERT INTO genres (name) VALUES
    ('Action'),
    ('Adventure'),
    ('Animation'),
    ('Comedy'),
    ('Drama'),
    ('Horror'),
    ('Romance'),
    ('Sci-Fi'),
    ('Thriller'),
    ('Documentary');

INSERT INTO halls (name, rows, seats_per_row) VALUES
    ('Hall 1', 8, 12),
    ('Hall 2', 6, 10);

-- One seat per (row, number) for every hall: rows A, B, C ... and numbers 1..seats_per_row.
INSERT INTO seats (hall_id, row_label, number)
SELECT h.id, chr(64 + r), n
FROM halls h
CROSS JOIN LATERAL generate_series(1, h.rows) AS r
CROSS JOIN LATERAL generate_series(1, h.seats_per_row) AS n;
