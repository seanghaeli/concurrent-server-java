package cpen221.mp3.fsftbuffer;

public class Page implements Bufferable{
    private final String title;
    private final String content;
    /**
     * Rep Invariant:
     * title and content are not null
     */
    /**
     * Abstraction Function:
     * Represents a Wikipedia page with its title and contents
     */

    /**
     * Creates an instance of Page with given title and content
     * @param title the title/id of the Page instance
     * @param content the content corresponding to the page title.
     */
    public Page(String title, String content) {
        this.title = title;
        this.content = content;
    }

    @Override
    public String id() {
        return title;
    }

    /**
     * Returns the content of the Page
     * @return content
     */
    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Page)) {
            return false;
        }
        Page that = (Page) obj;
        return this.title.equals(that.title) && this.content.equals(that.content);
    }
}
