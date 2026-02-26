package com.scibite.pipelines;

/**
 * Classe que representa um item de notícia.
 *
 * Contém o título (headline) e o link da notícia.
 * É uma classe imutável para garantir a integridade dos dados.
 */
public class NewsItem {

    // Título/headline da notícia
    private final String title;

    // Link/URL da notícia
    private final String link;

    /**
     * Construtor do item de notícia.
     *
     * @param title Título da notícia
     * @param link URL da notícia
     */
    public NewsItem(String title, String link) {
        this.title = title;
        this.link = link;
    }

    /**
     * Retorna o título da notícia.
     *
     * @return Título da notícia
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retorna o link da notícia.
     *
     * @return URL da notícia
     */
    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return String.format("NewsItem{title='%s', link='%s'}", title, link);
    }
}

