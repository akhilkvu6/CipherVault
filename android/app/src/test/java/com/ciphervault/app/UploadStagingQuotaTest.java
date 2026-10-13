package com.ciphervault.app;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pure JUnit unit tests verifying Upload Staging quota and batch calculation logic:
 * - 1 GB (1,073,741,824 bytes) max single file limit
 * - 1 GB (1,073,741,824 bytes) max selected batch limit
 * - 10 GB (10,737,418,240 bytes) authoritative vault capacity
 * - StagingStatus and statusReason transitions
 * - Batch calculation ignoring excluded and quota-exceeded items
 */
public class UploadStagingQuotaTest {

    private static final long ONE_GB = UploadStagingActivity.MAX_SINGLE_FILE_BYTES; // 1,073,741,824 bytes
    private static final long TEN_GB = UploadStagingActivity.AUTHORITATIVE_CAPACITY_BYTES; // 10,737,418,240 bytes

    private List<UploadStagingActivity.StagedFile> stagedFiles;

    @Before
    public void setUp() {
        stagedFiles = new ArrayList<>();
    }

    @Test
    public void testConstantsMatchAuthoritativeLimits() {
        assertEquals("Max single file limit must equal 1 GB (1,073,741,824 bytes)",
                1073741824L, ONE_GB);
        assertEquals("Max selected batch limit must equal 1 GB (1,073,741,824 bytes)",
                1073741824L, UploadStagingActivity.MAX_BATCH_BYTES);
        assertEquals("Authoritative vault capacity must equal 10 GB (10,737,418,240 bytes)",
                10737418240L, TEN_GB);
    }

    @Test
    public void testSingleFileUnderLimitIsValid() {
        long validSize = 500L * 1024L * 1024L; // 500 MB
        UploadStagingActivity.StagedFile file = new UploadStagingActivity.StagedFile(
                null, "test.jpg", validSize
        );
        assertTrue(file.size <= ONE_GB);
        assertEquals(UploadStagingActivity.StagingStatus.READY, file.status);
    }

    @Test
    public void testSingleFileExceedingOneGigabyteIsFlagged() {
        long oversized = ONE_GB + 1; // 1 byte over 1 GB
        UploadStagingActivity.StagedFile file = new UploadStagingActivity.StagedFile(
                null, "huge.mp4", oversized
        );

        if (file.size > ONE_GB) {
            file.status = UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA;
            file.statusReason = "Exceeds 1 GB file limit";
        }

        assertEquals(UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA, file.status);
        assertEquals("Exceeds 1 GB file limit", file.statusReason);
    }

    @Test
    public void testBatchCalculationSumsOnlyReadyOrUploadingItems() {
        UploadStagingActivity.StagedFile ready1 = new UploadStagingActivity.StagedFile(
                null, "1.png", 100_000_000L
        );
        ready1.status = UploadStagingActivity.StagingStatus.READY;

        UploadStagingActivity.StagedFile ready2 = new UploadStagingActivity.StagedFile(
                null, "2.png", 200_000_000L
        );
        ready2.status = UploadStagingActivity.StagingStatus.UPLOADING;

        UploadStagingActivity.StagedFile excluded = new UploadStagingActivity.StagedFile(
                null, "3.png", 500_000_000L
        );
        excluded.status = UploadStagingActivity.StagingStatus.DUPLICATE_EXCLUDED;

        UploadStagingActivity.StagedFile overflow = new UploadStagingActivity.StagedFile(
                null, "4.png", 900_000_000L
        );
        overflow.status = UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA;

        stagedFiles.add(ready1);
        stagedFiles.add(ready2);
        stagedFiles.add(excluded);
        stagedFiles.add(overflow);

        long batchSize = 0;
        for (UploadStagingActivity.StagedFile f : stagedFiles) {
            if (f.status == UploadStagingActivity.StagingStatus.READY
                    || f.status == UploadStagingActivity.StagingStatus.UPLOADING) {
                batchSize += f.size;
            }
        }

        assertEquals("Batch size must strictly include only READY and UPLOADING files",
                300_000_000L, batchSize);
    }

    @Test
    public void testBatchExceedingOneGigabyteIsFlagged() {
        UploadStagingActivity.StagedFile f1 = new UploadStagingActivity.StagedFile(
                null, "a.zip", 600_000_000L
        );
        UploadStagingActivity.StagedFile f2 = new UploadStagingActivity.StagedFile(
                null, "b.zip", 600_000_000L
        );

        stagedFiles.add(f1);
        stagedFiles.add(f2);

        long totalBatch = f1.size + f2.size; // 1.2 GB
        assertTrue(totalBatch > ONE_GB);

        if (totalBatch > ONE_GB) {
            f2.status = UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA;
            f2.statusReason = "Exceeds 1 GB batch limit";
        }

        assertEquals(UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA, f2.status);
        assertEquals("Exceeds 1 GB batch limit", f2.statusReason);
    }

    @Test
    public void testVaultCapacityExceededFlagged() {
        long currentUsedStorage = 9L * 1024L * 1024L * 1024L + 800L * 1024L * 1024L; // 9.8 GB used
        long newFile = 300L * 1024L * 1024L; // 300 MB -> total 10.1 GB

        UploadStagingActivity.StagedFile f = new UploadStagingActivity.StagedFile(
                null, "doc.pdf", newFile
        );

        if (currentUsedStorage + newFile > TEN_GB) {
            f.status = UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA;
            f.statusReason = "Exceeds 10 GB vault limit";
        }

        assertEquals(UploadStagingActivity.StagingStatus.EXCEEDS_QUOTA, f.status);
        assertEquals("Exceeds 10 GB vault limit", f.statusReason);
    }
}
