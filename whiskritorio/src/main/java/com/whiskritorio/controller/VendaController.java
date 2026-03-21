package com.whiskritorio.controller;

import com.whiskritorio.dao.ClienteRepository;
import com.whiskritorio.dao.ProdutoRepository;
import com.whiskritorio.dao.VendaRepository;
import com.whiskritorio.model.Cliente;
import com.whiskritorio.model.FormaPagamento;
import com.whiskritorio.model.Produto;
import com.whiskritorio.model.Venda;
import com.whiskritorio.model.VendaItem;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VendaController {

    private static final int PAGE_SIZE = 1;

    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public VendaController(VendaRepository vendaRepository,
                           ClienteRepository clienteRepository,
                           ProdutoRepository produtoRepository) {
        this.vendaRepository = vendaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    @GetMapping("/vendas")
    public String listar(@RequestParam(defaultValue = "0") int page, Model model) {
        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Venda> vendasPage = vendaRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("criadoEm").descending()));

        model.addAttribute("vendasPage", vendasPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("venda", new Venda());
        model.addAttribute("editando", false);
        carregarCombos(model);
        return "view/vendas";
    }

    @GetMapping("/vendas/editar/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Optional<Venda> vendaOpt = vendaRepository.findById(id);
        if (vendaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Venda não encontrada.");
            return "redirect:/vendas?page=" + page;
        }

        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);

        Page<Venda> vendasPage = vendaRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("criadoEm").descending()));

        model.addAttribute("vendasPage", vendasPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("venda", vendaOpt.get());
        model.addAttribute("editando", true);
        carregarCombos(model);
        return "view/vendas";
    }

    @GetMapping("/vendas/nota/{id}")
    public String nota(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Venda> vendaOpt = vendaRepository.findById(id);
        if (vendaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Venda não encontrada.");
            return "redirect:/vendas";
        }
        model.addAttribute("venda", vendaOpt.get());
        return "view/nota";
    }

    @PostMapping("/vendas/salvar")
    @Transactional
    public String salvar(@RequestParam(required = false) Long id,
                         @RequestParam Long clienteId,
                         @RequestParam FormaPagamento formaPagamento,
                         @RequestParam(required = false) List<Long> produtoId,
                         @RequestParam(required = false) List<Integer> quantidade,
                         @RequestParam(defaultValue = "0") int page,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isEmpty()) {
            model.addAttribute("erro", "Cliente inválido.");
            prepararTelaComErro(model, page);
            return "view/vendas";
        }

        if (id != null) {
            Optional<Venda> vendaOpt = vendaRepository.findById(id);
            if (vendaOpt.isEmpty()) {
                model.addAttribute("erro", "Venda não encontrada.");
                prepararTelaComErro(model, page);
                return "view/vendas";
            }
            Venda venda = vendaOpt.get();
            venda.setCliente(clienteOpt.get());
            venda.setFormaPagamento(formaPagamento);
            venda.setTotal(calcularTotal(venda.getItens()));
            vendaRepository.save(venda);
            redirectAttributes.addFlashAttribute("sucesso", "Venda atualizada. Itens não podem ser alterados.");
            return "redirect:/vendas?page=" + page;
        }

        if (produtoId == null || produtoId.isEmpty() || quantidade == null || quantidade.isEmpty()) {
            model.addAttribute("erro", "Adicione pelo menos um produto.");
            prepararTelaComErro(model, page);
            return "view/vendas";
        }

        int totalItens = Math.min(produtoId.size(), quantidade.size());
        List<VendaItem> itens = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < totalItens; i++) {
            Long pid = produtoId.get(i);
            Integer qtd = quantidade.get(i);

            if (pid == null || qtd == null || qtd <= 0) {
                continue;
            }

            Optional<Produto> produtoOpt = produtoRepository.findById(pid);
            if (produtoOpt.isEmpty()) {
                continue;
            }

            Produto produto = produtoOpt.get();
            if (produto.getEstoque() != null && produto.getEstoque() < qtd) {
                model.addAttribute("erro", "Estoque insuficiente para o produto: " + produto.getNome());
                prepararTelaComErro(model, page);
                return "view/vendas";
            }

            BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(qtd));
            total = total.add(subtotal);

            VendaItem item = new VendaItem();
            item.setProduto(produto);
            item.setQuantidade(qtd);
            item.setPrecoUnitario(produto.getPreco());
            item.setSubtotal(subtotal);
            itens.add(item);
        }

        if (itens.isEmpty()) {
            model.addAttribute("erro", "Adicione pelo menos um produto válido.");
            prepararTelaComErro(model, page);
            return "view/vendas";
        }

        Venda venda = new Venda();
        venda.setCliente(clienteOpt.get());
        venda.setFormaPagamento(formaPagamento);
        venda.setTotal(total);

        for (VendaItem item : itens) {
            venda.addItem(item);
        }

        vendaRepository.save(venda);
        redirectAttributes.addFlashAttribute("sucesso", "Venda salva com sucesso.");
        return "redirect:/vendas?page=" + page;
    }

    @PostMapping("/vendas/excluir/{id}")
    @Transactional
    public String excluir(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirectAttributes) {
        Optional<Venda> vendaOpt = vendaRepository.findById(id);
        if (vendaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Venda não encontrada.");
            return "redirect:/vendas?page=" + page;
        }
        Venda venda = vendaOpt.get();
        recomporEstoque(venda);
        vendaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("sucesso", "Venda excluída com sucesso.");
        return "redirect:/vendas?page=" + page;
    }

    private BigDecimal calcularTotal(List<VendaItem> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (VendaItem item : itens) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    private void recomporEstoque(Venda venda) {
        for (VendaItem item : venda.getItens()) {
            Produto produto = item.getProduto();
            if (produto.getEstoque() != null) {
                produto.setEstoque(produto.getEstoque() + item.getQuantidade());
                produtoRepository.save(produto);
            }
        }
    }

    private void prepararTelaComErro(Model model, int page) {
        int lastPage = calcularUltimaPagina();
        int safePage = normalizarPagina(page, lastPage);
        Page<Venda> vendasPage = vendaRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by("criadoEm").descending()));

        model.addAttribute("vendasPage", vendasPage);
        model.addAttribute("lastPage", lastPage);
        model.addAttribute("venda", new Venda());
        model.addAttribute("editando", false);
        carregarCombos(model);
    }

    private void carregarCombos(Model model) {
        model.addAttribute("clientes", clienteRepository.findAll(Sort.by("nome").ascending()));
        model.addAttribute("produtos", produtoRepository.findAll(Sort.by("nome").ascending()));
        model.addAttribute("formasPagamento", FormaPagamento.values());
    }

    private int calcularUltimaPagina() {
        long total = vendaRepository.count();
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
