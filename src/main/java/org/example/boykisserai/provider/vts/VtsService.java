package org.example.boykisserai.provider.vts;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VtsService {

    private final AppProperties props;
    private final VtsClient vtsClient = new VtsClient();

    public VtsService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        String url = props.getVts().getWebsocketUrl();
        log.info("[VTS] Подключение к VTube Studio по адресу: {}", url);
        vtsClient.connect(url);
    }

    public void sendRawJson(String jsonPayload) {
        vtsClient.send(jsonPayload);
    }
}