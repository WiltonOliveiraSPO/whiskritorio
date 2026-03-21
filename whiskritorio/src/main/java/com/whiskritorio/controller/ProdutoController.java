package com.whiskritorio.controller;

import com.whiskritorio.dao.ProdutoRepository;
import com.whiskritorio.model.Produto;
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
public class ProdutoController {

    private static final int PAGE_SIZE = 1;

    private final ProdutoRepository produtoRepository;

    public ProdutoController(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @GetMapping("/produtos")
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Produto> produtosPage = produtoRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));

        model.addAttribute("produtosPage", produtosPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("produto", new Produto());
        model.addAttribute("editando", false);
        return "view/produtos";
    }

    @GetMapping("/produtos/editar/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Optional<Produto> produtoOpt = produtoRepository.findById(id);
        if (produtoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
            return "redirect:/produtos?page=" + page;
        }

        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Produto> produtosPage = produtoRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));

        model.addAttribute("produtosPage", produtosPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("produto", produtoOpt.get());
        model.addAttribute("editando", true);
        return "view/produtos";
    }

    @PostMapping("/produtos/salvar")
    public String salvar(@Valid Produto produto,
                         BindingResult bindingResult,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            int lastPage = calcularUltimaPagina();
            int safePage = normalizarPagina(page, lastPage);

            Page<Produto> produtosPage = produtoRepository.findAll(
                    PageRequest.of(safePage, PAGE_SIZE, Sort.by("nome").ascending()));
            model.addAttribute("produtosPage", produtosPage);
            model.addAttribute("lastPage", lastPage);
            model.addAttribute("editando", produto.getId() != null);
            return "view/produtos";
        }

        produtoRepository.save(produto);
        redirectAttributes.addFlashAttribute("sucesso", "Produto salvo com sucesso.");
        return "redirect:/produtos?page=" + page;
    }

    @PostMapping("/produtos/excluir/{id}")
    public String excluir(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        if (!produtoRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("erro", "Produto não encontrado.");
            return "redirect:/produtos?page=" + page;
        }
        produtoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("sucesso", "Produto excluído com sucesso.");
        return "redirect:/produtos?page=" + page;
    }

    private int calcularUltimaPagina() {
        long total = produtoRepository.count();
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
