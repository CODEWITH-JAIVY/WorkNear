package com.labourse.notification.event;

import com.labourse.notification.websocket.PushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobEventListenerTest {

    @Mock PushService pushService;
    @InjectMocks JobEventListener listener;

    @Test
    void onJobPosted_pushesToEveryCandidateLabour() throws Exception {
        Map<String, Object> event = Map.of(
                "jobId", 42, "candidateLabourIds", List.of("101", "102", "103"));

        listener.onJobPosted(event);

        verify(pushService).pushToUser(eq("101"), contains("NEW_JOB_AVAILABLE"));
        verify(pushService).pushToUser(eq("102"), contains("NEW_JOB_AVAILABLE"));
        verify(pushService).pushToUser(eq("103"), contains("NEW_JOB_AVAILABLE"));
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void onJobPosted_pushesToNoOne_whenCandidateListIsNull() throws Exception {
        listener.onJobPosted(Map.of("jobId", 42));
        verifyNoInteractions(pushService);
    }

    @Test
    void onJobInterestExpressed_notifiesOnlyTheCustomer() throws Exception {
        Map<String, Object> event = Map.of("jobId", 42, "customerId", 7, "labourId", 101);

        listener.onJobInterestExpressed(event);

        verify(pushService).pushToUser(eq("7"), contains("NEW_INTEREST"));
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void onJobAssigned_notifiesWinnerAndEveryoneElseDifferently() throws Exception {
        Map<String, Object> event = Map.of(
                "jobId", 42, "customerId", 7, "labourId", 101,
                "notSelectedLabourIds", List.of(102, 103));

        listener.onJobAssigned(event);

        verify(pushService).pushToUser(eq("101"), contains("JOB_ASSIGNED_TO_YOU"));
        verify(pushService).pushToUser(eq("102"), contains("JOB_FILLED_BY_ANOTHER"));
        verify(pushService).pushToUser(eq("103"), contains("JOB_FILLED_BY_ANOTHER"));
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void onJobAssigned_stillNotifiesWinner_whenNoOneElseWasInterested() throws Exception {
        Map<String, Object> event = Map.of("jobId", 42, "customerId", 7, "labourId", 101);

        listener.onJobAssigned(event);

        verify(pushService).pushToUser(eq("101"), contains("JOB_ASSIGNED_TO_YOU"));
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void onBulkJobPosted_pushesToEveryMatchedWorker() throws Exception {
        Map<String, Object> event = Map.of(
                "bulkJobId", 50, "candidateLabourIds", List.of("201", "202", "203"));

        listener.onBulkJobPosted(event);

        verify(pushService).pushToUser(eq("201"), contains("BULK_JOB_AVAILABLE"));
        verify(pushService).pushToUser(eq("202"), contains("BULK_JOB_AVAILABLE"));
        verify(pushService).pushToUser(eq("203"), contains("BULK_JOB_AVAILABLE"));
        verifyNoMoreInteractions(pushService);
    }

    @Test
    void onBulkJobPosted_pushesToNoOne_whenNoCandidatesMatched() throws Exception {
        listener.onBulkJobPosted(Map.of("bulkJobId", 50));
        verifyNoInteractions(pushService);
    }
}
