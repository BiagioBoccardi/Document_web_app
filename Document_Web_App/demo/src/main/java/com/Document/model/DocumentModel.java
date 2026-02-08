package main.java.com.Document.model;
import java.time.LocalDate;

public class DocumentModel {
    private int id;
    private String titolo;
    private LocalDate dataModifica;
    private String tipologia;

    public DocumentModel(LocalDate dataModifica, int id, String tipologia, String titolo) {
        this.dataModifica = dataModifica;
        this.id = id;
        this.tipologia = tipologia;
        this.titolo = titolo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public LocalDate getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(LocalDate dataModifica) {
        this.dataModifica = dataModifica;
    }

    public String getTipologia() {
        return tipologia;
    }

    public void setTipologia(String tipologia) {
        this.tipologia = tipologia;
    }



}
