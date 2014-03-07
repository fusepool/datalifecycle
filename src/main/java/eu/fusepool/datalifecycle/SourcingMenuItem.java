package eu.fusepool.datalifecycle;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

@Component
@Service(NavigationLink.class)
public class SourcingMenuItem extends NavigationLink {
    
    public SourcingMenuItem() {
        super("sourcing/", "/Sourcing", "The data sourcing manager", 300);
    }
    
}
