package luzes.coresystemstgr.porta;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@JsonPropertyOrder({"id", "cardUid", "nome", "ocorridoEm", "sincronizadoEm", "offline"})
@Entity
@Table(name = "entrada_porta")
public class EntradaPorta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_uid", length = 64)
    @Schema(description = "UID do cartão RFID em hexadecimal")
    private String cardUid;

    @Transient
    @Schema(description = "Nome cadastrado no site para este cartão")
    private String nome;

    @Column(name = "ocorrido_em", nullable = false)
    @Schema(description = "Quando a entrada ocorreu (Java ou enviada pelo ESP após NTP)")
    private LocalDateTime ocorridoEm;

    @Column(name = "sincronizado_em", nullable = false)
    @Schema(description = "Quando o servidor Java recebeu o registro")
    private LocalDateTime sincronizadoEm;

    @Column(name = "offline", nullable = false)
    @Schema(description = "true se a entrada ficou na fila local do ESP sem internet")
    private boolean offline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardUid() {
        return cardUid;
    }

    public void setCardUid(String cardUid) {
        this.cardUid = cardUid;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(LocalDateTime ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public LocalDateTime getSincronizadoEm() {
        return sincronizadoEm;
    }

    public void setSincronizadoEm(LocalDateTime sincronizadoEm) {
        this.sincronizadoEm = sincronizadoEm;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }
}
