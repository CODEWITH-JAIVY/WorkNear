package com.labourse.chat.controller;

import com.labourse.chat.entity.ChatMessage;
import com.labourse.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatMessageRepository repository;

    // TODO: verify the requesting X-User-Id is actually a participant in this job (customer or
    // the accepted labour) before returning history — not enforced yet in this pass.
    @GetMapping("/jobs/{jobId}/messages")
    public List<ChatMessage> history(@PathVariable Long jobId) {
        return repository.findByJobIdOrderBySentAtAsc(jobId);
    }
}
