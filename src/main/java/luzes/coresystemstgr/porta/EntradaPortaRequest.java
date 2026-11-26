package luzes.coresystemstgr.porta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class EntradaPortaRequest {

    @Schema(description = "UID do cartão RFID (hex)", example = "A1B2C3D4")
    private String cardUid;

    @Schema(description = "Momento da entrada no ESP (opcional). Se omitido, o Java registra a hora atual.")
    private LocalDateTime ocorridoEm;

    @Schema(description = "true se veio da fila offline do ESP", example = "false")
    private Boolean offline;

    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }

    public LocalDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(LocalDateTime ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public Boolean getOffline() {
        return offline;
    }

    public void setOffline(Boolean offline) {
        this.offline = offline;
    }
}
