package luzes.coresystemstgr.porta;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@JsonPropertyOrder({"cardUid", "nome"})
@Entity
@Table(name = "cartao_pessoa")
public class CartaoPessoa {

    @Id
    @Column(name = "card_uid", length = 64, nullable = false)
    @Schema(description = "UID do cartão RFID em hexadecimal")
    private String cardUid;

    @Column(name = "nome", length = 120, nullable = false)
    @Schema(description = "Nome da pessoa que usa o cartão")
    private String nome;

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
}
