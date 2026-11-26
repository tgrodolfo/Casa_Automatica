package luzes.coresystemstgr.porta;

import io.swagger.v3.oas.annotations.media.Schema;

public class CartaoPessoaRequest {

    @Schema(description = "Nome da pessoa", example = "Maria Silva")
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
