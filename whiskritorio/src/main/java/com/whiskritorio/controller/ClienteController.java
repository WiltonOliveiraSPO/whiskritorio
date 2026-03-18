package com.whiskritorio.controller;

import com.whiskritorio.dao.ClienteRepository;
import com.whiskritorio.model.Cliente;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClienteController {

    private static final int PAGE_SIZE = 1;

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/clientes")
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Cliente> clientesPage = clienteRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));

        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("editando", false);
        return "view/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(id);
        if (clienteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes?page=" + page;
        }

        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Cliente> clientesPage = clienteRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));

        model.addAttribute("clientesPage", clientesPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("cliente", clienteOpt.get());
        model.addAttribute("editando", true);
        return "view/clientes";
    }

    @PostMapping("/clientes/salvar")
    public String salvar(@Valid Cliente cliente,
                         BindingResult bindingResult,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            int lastPage = calcularUltimaPagina();
            int safePage = normalizarPagina(page, lastPage);

            Page<Cliente> clientesPage = clienteRepository.findAll(
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));
            model.addAttribute("clientesPage", clientesPage);
            model.addAttribute("lastPage", lastPage);
            model.addAttribute("editando", cliente.getId() != null);
            return "view/clientes";
        }

        clienteRepository.save(cliente);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente salvo com sucesso.");
        return "redirect:/clientes?page=" + page;
    }

    @PostMapping("/clientes/excluir/{id}")
    public String excluir(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        if (!clienteRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes?page=" + page;
        }
        clienteRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("sucesso", "Cliente excluído com sucesso.");
        return "redirect:/clientes?page=" + page;
    }

    private int calcularUltimaPagina() {
        long total = clienteRepository.count();
        if (total == 0) {
            return 0;
        }
        return (int) Math.max((total - 1) / PAGE_SIZE, 0);
    }

    private int normalizarPagina(int page, int lastPage) {
        if (page < 0) {
            return 0;
        }
        if (page > lastPage) {
            return lastPage;
        }
        return page;
    }
}
