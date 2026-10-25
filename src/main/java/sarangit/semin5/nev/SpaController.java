package sarangit.semin5.nev;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/admin"})
    public String index() { return "forward:/index.html"; }
}
