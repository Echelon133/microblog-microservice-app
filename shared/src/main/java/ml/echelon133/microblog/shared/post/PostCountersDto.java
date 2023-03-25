package ml.echelon133.microblog.shared.post;

public class PostCountersDto {

    private Long likes;
    private Long quotes;
    private Long responses;

    public PostCountersDto() {}
    public PostCountersDto(Long likes, Long quotes, Long responses) {
        this.likes = likes;
        this.quotes = quotes;
        this.responses = responses;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public Long getQuotes() {
        return quotes;
    }

    public void setQuotes(Long quotes) {
        this.quotes = quotes;
    }

    public Long getResponses() {
        return responses;
    }

    public void setResponses(Long responses) {
        this.responses = responses;
    }
}
