package luzes.coresystemstgr.chuva;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@JsonPropertyOrder({"id", "inicio", "fim", "emAndamento"})
@Entity
@Table(name = "momento_chuva")
public class MomentoChuva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inicio", nullable = false)
    @Schema(description = "Data/hora em que a chuva começou (registrada pelo servidor Java)")
    private LocalDateTime inicio;

    @Column(name = "fim")
    @Schema(description = "Data/hora em que a chuva acabou; null se ainda chovendo")
    private LocalDateTime fim;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isEmAndamento() {
        return fim == null;
    }
}
