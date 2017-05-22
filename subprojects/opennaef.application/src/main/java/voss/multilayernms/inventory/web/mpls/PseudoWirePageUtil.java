package voss.multilayernms.inventory.web.mpls;

import naef.dto.mpls.PseudowireDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;

public class PseudoWirePageUtil {

    public static Link<Void> createPwEditLink(final String id, final WebPage backPage, final PseudowireDto pw) {
        Link<Void> link = new Link<Void>(id) {
            private static final long serialVersionUID = 1L;

            public void onClick() {
                PseudoWireEditPage page = new PseudoWireEditPage(backPage, pw);
                setResponsePage(page);
            }
        };
        link.setEnabled(pw != null);
        return link;
    }

}