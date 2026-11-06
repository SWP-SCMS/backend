# Image storage operations

Mount durable storage for both PROFILE_IMAGES_DIRECTORY and SCMS_COURSE_IMAGES_DIRECTORY.
All backend replicas must see the same directories. Back up these volumes with the database.
The default ./data directories are suitable for local development only on ephemeral hosts.

## Orphan course images

Run cleanup during a maintenance window with application writes stopped.
Export a fresh JSON array of every courses.image_url from the same database:
SELECT coalesce(json_agg(image_url), '[]'::json) FROM courses;
Save the result as references.json, without column headers.

Preview: python scripts/clean-course-images.py /absolute/course-images references.json
After reviewing the candidates, rerun with --delete while writes remain stopped.
Only UUID JPEG/PNG files older than seven days and absent from the export are eligible.
Symlinks and unrelated files are ignored. Keep a volume backup for recovery.
Never use a stale export or resume writes between export and deletion.

## Coach leave pagination

GET /manager/coach-leave-requests?page=0&size=20&status=PENDING returns a page.
Size is limited to 100; ordering is createdAt then id. The frontend uses this contract.
Requests without page retain the legacy array response for existing clients.
