package com.ciphervault.app.transfer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TransferModelTest {

    @Test
    public void testTransferItemProgressAndFormatting() {
        TransferItem item = new TransferItem(
                "batch-1",
                TransferType.UPLOAD,
                "presentation.pdf",
                null,
                null,
                1000000L // 1 MB
        );

        assertEquals("batch-1", item.getBatchId());
        assertEquals("presentation.pdf", item.getFileName());
        assertEquals(TransferType.UPLOAD, item.getType());
        assertEquals(TransferState.QUEUED, item.getState());
        assertEquals(0, item.getProgressPercentage());

        item.setTransferredBytes(500000L);
        assertEquals(50, item.getProgressPercentage());

        // Speed formatting
        item.setSpeedBytesPerSecond(2048000L); // ~1.95 MB/s
        assertTrue(item.getFormattedSpeed().contains("MB/s"));

        item.setSpeedBytesPerSecond(512000L); // 500 KB/s
        assertTrue(item.getFormattedSpeed().contains("KB/s"));

        // ETA formatting
        item.setEtaSeconds(45);
        assertEquals("ETA 00:45", item.getFormattedEta());

        item.setEtaSeconds(125);
        assertEquals("ETA 02:05", item.getFormattedEta());

        item.setEtaSeconds(3665);
        assertEquals("ETA 01:01:05", item.getFormattedEta());

        // Cancellation
        assertFalse(item.isCancelled());
        item.cancel();
        assertTrue(item.isCancelled());
        assertEquals(TransferState.CANCELLED, item.getState());
    }

    @Test
    public void testTransferBatchMultiFileAggregation() {
        List<TransferItem> items = new ArrayList<>();
        items.add(new TransferItem("batch-multi", TransferType.UPLOAD, "file1.png", null, null, 1000L));
        items.add(new TransferItem("batch-multi", TransferType.UPLOAD, "file2.png", null, null, 2000L));
        items.add(new TransferItem("batch-multi", TransferType.UPLOAD, "file3.png", null, null, 3000L));

        TransferBatch batch = new TransferBatch(TransferType.UPLOAD, "Photo Batch", items);
        assertEquals(3, batch.getTotalCount());
        assertEquals(6000L, batch.getTotalBytes());
        assertEquals(0, batch.getCompletedCount());
        assertEquals(0, batch.getOverallProgress());

        // Item 1 completes
        items.get(0).setTransferredBytes(1000L);
        items.get(0).setState(TransferState.COMPLETED);
        assertEquals(1, batch.getCompletedCount());

        // Item 2 at 50%
        items.get(1).setTransferredBytes(1000L);
        items.get(1).setState(TransferState.UPLOADING);

        // Overall bytes transferred = 1000 + 1000 = 2000 / 6000 = 33%
        assertEquals(33, batch.getOverallProgress());
        assertEquals("file2.png", batch.getCurrentItem().getFileName());
        assertEquals(2, batch.getCurrentItemIndex());

        // Batch cancellation
        batch.cancel();
        assertEquals(TransferState.CANCELLED, batch.getState());
        assertEquals(TransferState.CANCELLED, items.get(1).getState());
        assertEquals(TransferState.CANCELLED, items.get(2).getState());
        // Previously completed item remains completed
        assertEquals(TransferState.COMPLETED, items.get(0).getState());
    }

    @Test
    public void testNotificationThrottlingLogic() {
        // First update should always pass
        assertTrue(TransferNotificationHelper.shouldUpdateProgressNotification(10));

        // Rapid same-percentage update within <350ms should be throttled
        assertFalse(TransferNotificationHelper.shouldUpdateProgressNotification(10));

        // Jump of >= 3% should pass
        assertTrue(TransferNotificationHelper.shouldUpdateProgressNotification(14));

        // 100% completion should always pass
        assertTrue(TransferNotificationHelper.shouldUpdateProgressNotification(100));
    }
}
