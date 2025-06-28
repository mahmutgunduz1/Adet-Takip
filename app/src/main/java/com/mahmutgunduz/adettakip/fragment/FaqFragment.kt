package com.mahmutgunduz.adettakip.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.adapter.FaqAdapter
import com.mahmutgunduz.adettakip.databinding.FragmentFaqBinding
import com.mahmutgunduz.adettakip.model.FaqItem

class FaqFragment : Fragment() {

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding!!
    private lateinit var faqAdapter: FaqAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        loadFaqData()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Sıkça Sorulan Sorular"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
    }

    private fun setupRecyclerView() {
        faqAdapter = FaqAdapter { faqItem ->
            // FAQ item tıklandığında expand/collapse
            faqItem.isExpanded = !faqItem.isExpanded
            faqAdapter.notifyDataSetChanged()
        }

        binding.rvFaq.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = faqAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadFaqData() {
        val faqList = listOf(
            FaqItem(
                "Adet döngüm düzensiz, uygulama doğru tahmin yapabilir mi?",
                "Uygulama en az 3 aylık düzenli veri ile daha doğru tahminler yapar. Düzensiz döngüleriniz varsa doktorunuza danışmanızı öneririz. Uygulama sadece genel tahmin verir, kesin sonuç değildir."
            ),
            FaqItem(
                "SCS saati nedir ve nasıl hesaplanır?",
                "SCS (Saat olarak Adet Ortalaması) = AOs - 365 formülü ile hesaplanır. Bu, ovülasyon zamanını tahmin etmek için kullanılan bir yöntemdir. Ancak bu sadece bir tahmindir ve %100 güvenilir değildir."
            ),
            FaqItem(
                "Güvenli günler gerçekten güvenli mi?",
                "Uygulamada gösterilen 'güvenli günler' sadece tahmini dönemleri gösterir. Hiçbir doğal yöntem %100 güvenli değildir. Kesin korunma için ek yöntemler kullanmanız önerilir."
            ),
            FaqItem(
                "Verilerim güvende mi?",
                "Evet, tüm verileriniz Firebase güvenlik protokolleri ile korunmaktadır. Sadece sizin hesabınızla erişilebilir ve üçüncü kişilerle paylaşılmaz."
            ),
            FaqItem(
                "Kaç adet tarihi kaydetmeliyim?",
                "En az 2-3 aylık veri (6-9 adet tarihi) kaydetmeniz önerilir. Daha fazla veri, daha doğru tahminler sağlar."
            ),
            FaqItem(
                "Hamilelik durumunda uygulama nasıl etkilenir?",
                "Hamilelik süresince adet olmayacağı için uygulama tahmin yapamaz. Doğum sonrası düzenli adet görmeye başladığınızda tekrar kullanabilirsiniz."
            ),
            FaqItem(
                "Yaş faktörü hesaplamalarda etkili mi?",
                "Evet, yaş ilerledikçe döngüler değişebilir. Özellikle 40 yaş sonrası düzensizlikler artabilir. Bu durumda doktor kontrolü önemlidir."
            ),
            FaqItem(
                "Stres döngümü nasıl etkiler?",
                "Stres, hastalık, diyet değişiklikleri ve seyahat gibi faktörler döngünüzü etkileyebilir. Bu durumda tahminler daha az güvenilir olabilir."
            ),
            FaqItem(
                "Doğum kontrol hapı kullanırken uygulama kullanılabilir mi?",
                "Doğum kontrol hapı kullanırken doğal döngünüz olmadığı için uygulama doğru çalışmayabilir. Hap bıraktıktan sonra düzenli döngünüz başladığında kullanabilirsiniz."
            ),
            FaqItem(
                "Verilerimi nasıl yedekleyebilirim?",
                "Verileriniz Firebase hesabınızda otomatik olarak saklanır. Aynı hesapla farklı cihazlarda giriş yaparak verilerinize erişebilirsiniz."
            )
        )

        faqAdapter.updateList(faqList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}