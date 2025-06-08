package antix.components;

import antix.model.SocialMediaPost;
import com.vaadin.flow.component.html.Span;

public class PlatformBadge extends Span {
    
    public enum Size {
        SMALL("2px 6px", "10px", "12px"),
        MEDIUM("4px 8px", "12px", "16px"),
        LARGE("6px 12px", "14px", "20px");
        
        private final String padding;
        private final String fontSize;
        private final String borderRadius;
        
        Size(String padding, String fontSize, String borderRadius) {
            this.padding = padding;
            this.fontSize = fontSize;
            this.borderRadius = borderRadius;
        }
        
        public String getPadding() { return padding; }
        public String getFontSize() { return fontSize; }
        public String getBorderRadius() { return borderRadius; }
    }
    
    public enum Style {
        BADGE,      // Badge coloré avec emoji et nom
        CIRCLE,     // Badge rond avec initiale
        TEXT_ONLY   // Juste le nom de la plateforme
    }
    
    public PlatformBadge(SocialMediaPost post) {
        this(post, Size.SMALL, Style.BADGE);
    }
    
    public PlatformBadge(SocialMediaPost post, Size size, Style style) {
        switch (style) {
            case BADGE:
                createBadgeStyle(post, size);
                break;
            case CIRCLE:
                createCircleStyle(post, size);
                break;
            case TEXT_ONLY:
                createTextStyle(post, size);
                break;
        }
    }
    
    private void createBadgeStyle(SocialMediaPost post, Size size) {
        setText(post.getPlatformDisplayName());
        getStyle()
            .set("background-color", post.getBadgeColor())
            .set("color", post.getBadgeTextColor())
            .set("padding", size.getPadding())
            .set("border-radius", size.getBorderRadius())
            .set("font-size", size.getFontSize())
            .set("font-weight", "bold")
            .set("display", "inline-block")
            .set("white-space", "nowrap");
    }
    
    private void createCircleStyle(SocialMediaPost post, Size size) {
        setText(post.getPlatformShortCode());
        getStyle()
            .set("background-color", post.getBadgeColor())
            .set("color", post.getBadgeTextColor())
            .set("width", "24px")
            .set("height", "24px")
            .set("border-radius", "50%")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("font-weight", "bold")
            .set("font-size", size.getFontSize());
    }
    
    private void createTextStyle(SocialMediaPost post, Size size) {
        setText(post.getPlatformDisplayName());
        getStyle()
            .set("color", post.getBadgeColor())
            .set("font-size", size.getFontSize())
            .set("font-weight", "bold");
    }
}
