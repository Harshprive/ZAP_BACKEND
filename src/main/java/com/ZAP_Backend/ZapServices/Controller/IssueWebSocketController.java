package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.DataTransferObject.IssueWebSocketDTO;
import com.ZAP_Backend.ZapServices.Model.Issue;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Repository.IssueRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.Map;

@Controller
public class IssueWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProviderRepository providerRepository;

    public void broadcastNewIssue(Issue issue) {
        IssueWebSocketDTO dto = IssueWebSocketDTO.fromIssue(issue, "NEW_ISSUE");
        // Broadcast to all providers subscribed to the service topic
        messagingTemplate.convertAndSend("/topic/service/" + issue.getService().getId() + "/issues", dto);
        
        // Notify the user that their issue has been created
        try {
            messagingTemplate.convertAndSend(
                "/topic/user/" + issue.getUser().getId() + "/issues",
                Map.of(
                    "message", "Your issue has been created and providers have been notified",
                    "issueId", issue.getId(),
                    "status", "CREATED",
                    "action", "NEW_ISSUE"
                )
            );
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not send issue creation notification to user: " + e.getMessage());
        }
    }

    @MessageMapping("/issue/response")
    public void handleIssueResponse(@Payload IssueWebSocketDTO response) {
        Optional<Issue> issueOpt = issueRepository.findById(response.getIssueId());
        Optional<ServiceProvider> providerOpt = providerRepository.findById(response.getProviderId());

        if (issueOpt.isPresent() && providerOpt.isPresent()) {
            Issue issue = issueOpt.get();
            ServiceProvider provider = providerOpt.get();

            switch (response.getAction()) {
                case "ACCEPT":
                    // Update issue with accepted provider
                    issue.setServiceProvider(provider);
                    issue.setStatus("ACCEPTED");
                    issueRepository.save(issue);

                    // Notify other providers that the issue is no longer available
                    IssueWebSocketDTO notification = IssueWebSocketDTO.fromIssue(issue, "ACCEPTED");
                    messagingTemplate.convertAndSend("/topic/service/" + issue.getService().getId() + "/issues", notification);

                    // Notify the user that their issue has been accepted
                    try {
                        messagingTemplate.convertAndSend(
                            "/topic/user/" + issue.getUser().getId() + "/issues",
                            Map.of(
                                "message", "Your issue has been accepted by " + provider.getProvider_name(),
                                "issueId", issue.getId(),
                                "status", "ACCEPTED",
                                "action", "ACCEPTED",
                                "providerId", provider.getId(),
                                "providerName", provider.getProvider_name()
                            )
                        );
                    } catch (Exception e) {
                        System.out.println("⚠️ Warning: Could not send acceptance notification to user: " + e.getMessage());
                    }
                    break;

                case "REJECT":
                    // Notify the user about rejection
                    try {
                        messagingTemplate.convertAndSend(
                            "/topic/user/" + issue.getUser().getId() + "/issues",
                            Map.of(
                                "message", "Your issue has been rejected by " + provider.getProvider_name(),
                                "issueId", issue.getId(),
                                "status", "REJECTED",
                                "action", "REJECTED",
                                "providerId", provider.getId(),
                                "providerName", provider.getProvider_name()
                            )
                        );
                    } catch (Exception e) {
                        System.out.println("⚠️ Warning: Could not send rejection notification to user: " + e.getMessage());
                    }
                    break;

                case "REATTACHMENT":
                    // Update issue status and reattachment flag
                    issue.setServiceProvider(provider);
                    issue.setStatus("REATTACHMENT_REQUESTED");
                    issue.setReattachment(true);
                    issueRepository.save(issue);

                    // Notify the user about reattachment request
                    try {
                        messagingTemplate.convertAndSend(
                            "/topic/user/" + issue.getUser().getId() + "/issues",
                            Map.of(
                                "message", "Provider " + provider.getProvider_name() + " has requested reattachment for your issue",
                                "issueId", issue.getId(),
                                "status", "REATTACHMENT_REQUESTED",
                                "action", "REATTACHMENT",
                                "providerId", provider.getId(),
                                "providerName", provider.getProvider_name()
                            )
                        );
                    } catch (Exception e) {
                        System.out.println("⚠️ Warning: Could not send reattachment notification to user: " + e.getMessage());
                    }
                    break;
            }
        }
    }
} 