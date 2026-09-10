package org.example.boykisserai.service.animation;

import org.example.boykisserai.provider.vts.VtsService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class VtsParameterInjector {

    private final VtsService vtsService;

    public VtsParameterInjector(VtsService vtsService) {
        this.vtsService = vtsService;
    }

    // Очередной каприз VTube Studio из-за которого пришлось навайбкодить этот класс,
    // чтобы синхронизировать анимации персонажа с голосом и поведением
    public void setExpression(String expressionName, boolean active) {
        String fileName = expressionName.endsWith(".exp3.json") ? expressionName : expressionName + ".exp3.json";

        String jsonPayload = String.format("""
                {
                  "apiName": "VTubeStudioPublicAPI",
                  "apiVersion": "1.0",
                  "requestID": "ExpToggle",
                  "messageType": "ExpressionActivationRequest",
                  "data": {
                    "expressionFile": "%s",
                    "active": %b
                  }
                }
                """, fileName, active);

        vtsService.sendRawJson(jsonPayload);
    }

    // Для наклона головы и открывания рта
    public void inject(String paramName, double value) {
        String jsonPayload = String.format(Locale.US, """
                {
                  "apiName": "VTubeStudioPublicAPI",
                  "apiVersion": "1.0",
                  "requestID": "Inject_%s",
                  "messageType": "InjectParameterDataRequest",
                  "data": {
                    "faceFound": true,
                    "mode": "set",
                    "parameterValues": [
                      {
                        "id": "%s",
                        "value": %.2f,
                        "weight": 1.0
                      }
                    ]
                  }
                }
                """, paramName, paramName, value);

        vtsService.sendRawJson(jsonPayload);
    }
}