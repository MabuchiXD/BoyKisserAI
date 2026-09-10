package org.example.boykisserai.provider.ai;

import org.example.boykisserai.domain.model.ChatContext;
import org.example.boykisserai.domain.model.ChatResponse;


public interface AiProvider {

    ChatResponse generate(ChatContext context);

    //На случай если захочу и модель менять в UI
    boolean supports(String providerName);
}