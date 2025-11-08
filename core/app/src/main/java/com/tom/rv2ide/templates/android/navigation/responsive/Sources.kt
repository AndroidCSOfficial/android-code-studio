/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.templates.android.navigation.responsive

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object ResponsiveSources {

  fun mainActivityKotlin(packageId: String): String =
      """
      package $packageId
      
      import android.os.Bundle
      import android.view.Menu
      import android.view.MenuItem
      import com.google.android.material.snackbar.Snackbar
      import com.google.android.material.navigation.NavigationView
      import androidx.navigation.findNavController
      import androidx.navigation.fragment.NavHostFragment
      import androidx.navigation.ui.AppBarConfiguration
      import androidx.navigation.ui.navigateUp
      import androidx.navigation.ui.setupActionBarWithNavController
      import androidx.navigation.ui.setupWithNavController
      import androidx.appcompat.app.AppCompatActivity
      import $packageId.databinding.ActivityMainBinding
      
      class MainActivity : AppCompatActivity() {
      
          private lateinit var appBarConfiguration: AppBarConfiguration
          private lateinit var binding: ActivityMainBinding
      
          override fun onCreate(savedInstanceState: Bundle?) {
              super.onCreate(savedInstanceState)
              binding = ActivityMainBinding.inflate(layoutInflater)
              setContentView(binding.root)
              setSupportActionBar(binding.appBarMain.toolbar)
      
              binding.appBarMain.fab?.setOnClickListener { view ->
                  Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                      .setAction("Action", null)
                      .setAnchorView(R.id.fab).show()
              }
      
              val navHostFragment =
                  (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
              val navController = navHostFragment.navController
      
              binding.navView?.let {
                  appBarConfiguration = AppBarConfiguration(
                      setOf(
                          R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings
                      ),
                      binding.drawerLayout
                  )
                  setupActionBarWithNavController(navController, appBarConfiguration)
                  it.setupWithNavController(navController)
              }
      
              binding.appBarMain.contentMain.bottomNavView?.let {
                  appBarConfiguration = AppBarConfiguration(
                      setOf(
                          R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow
                      )
                  )
                  setupActionBarWithNavController(navController, appBarConfiguration)
                  it.setupWithNavController(navController)
              }
          }
      
          override fun onCreateOptionsMenu(menu: Menu): Boolean {
              val result = super.onCreateOptionsMenu(menu)
              // Using findViewById because NavigationView exists in different layout files
              // between w600dp and w1240dp
              val navView: NavigationView? = findViewById(R.id.nav_view)
              if (navView == null) {
                  // The navigation drawer already has the items including the items in the overflow menu
                  // We only inflate the overflow menu if the navigation drawer isn't visible
                  menuInflater.inflate(R.menu.overflow, menu)
              }
              return result
          }
      
          override fun onOptionsItemSelected(item: MenuItem): Boolean {
              when (item.itemId) {
                  R.id.nav_settings -> {
                      val navController = findNavController(R.id.nav_host_fragment_content_main)
                      navController.navigate(R.id.nav_settings)
                  }
              }
              return super.onOptionsItemSelected(item)
          }
      
          override fun onSupportNavigateUp(): Boolean {
              val navController = findNavController(R.id.nav_host_fragment_content_main)
              return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
          }
      }
  """
          .trimIndent()

  fun mainActivityJava(packageId: String): String =
      """
      package $packageId;
      
      import android.os.Bundle;
      import android.view.MenuItem;
      import android.view.Menu;
      
      import com.google.android.material.bottomnavigation.BottomNavigationView;
      import com.google.android.material.snackbar.Snackbar;
      import com.google.android.material.navigation.NavigationView;
      
      import androidx.annotation.NonNull;
      import androidx.navigation.NavController;
      import androidx.navigation.Navigation;
      import androidx.navigation.fragment.NavHostFragment;
      import androidx.navigation.ui.AppBarConfiguration;
      import androidx.navigation.ui.NavigationUI;
      import androidx.appcompat.app.AppCompatActivity;
      
      import $packageId.databinding.ActivityMainBinding;
      
      public class MainActivity extends AppCompatActivity {
      
          private AppBarConfiguration mAppBarConfiguration;
      
          @Override
          protected void onCreate(Bundle savedInstanceState) {
              super.onCreate(savedInstanceState);
      
              ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
              setContentView(binding.getRoot());
      
              setSupportActionBar(binding.appBarMain.toolbar);
              if (binding.appBarMain.fab != null) {
                  binding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                          .setAction("Action", null).setAnchorView(R.id.fab).show());
              }
              NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
              assert navHostFragment != null;
              NavController navController = navHostFragment.getNavController();
      
              NavigationView navigationView = binding.navView;
              if (navigationView != null) {
                  mAppBarConfiguration = new AppBarConfiguration.Builder(
                          R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings)
                          .setOpenableLayout(binding.drawerLayout)
                          .build();
                  NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
                  NavigationUI.setupWithNavController(navigationView, navController);
              }
      
              BottomNavigationView bottomNavigationView = binding.appBarMain.contentMain.bottomNavView;
              if (bottomNavigationView != null) {
                  mAppBarConfiguration = new AppBarConfiguration.Builder(
                          R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow)
                          .build();
                  NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
                  NavigationUI.setupWithNavController(bottomNavigationView, navController);
              }
          }
      
          @Override
          public boolean onCreateOptionsMenu(Menu menu) {
              boolean result = super.onCreateOptionsMenu(menu);
              // Using findViewById because NavigationView exists in different layout files
              // between w600dp and w1240dp
              NavigationView navView = findViewById(R.id.nav_view);
              if (navView == null) {
                  // The navigation drawer already has the items including the items in the overflow menu
                  // We only inflate the overflow menu if the navigation drawer isn't visible
                  getMenuInflater().inflate(R.menu.overflow, menu);
              }
              return result;
          }
      
          @Override
          public boolean onOptionsItemSelected(@NonNull MenuItem item) {
              if (item.getItemId() == R.id.nav_settings) {
                  NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                  navController.navigate(R.id.nav_settings);
              }
              return super.onOptionsItemSelected(item);
          }
      
          @Override
          public boolean onSupportNavigateUp() {
              NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
              return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                      || super.onSupportNavigateUp();
          }
      }
  """
          .trimIndent()
}

// Fragments
object Fragment {

  fun templateTypeTransformKt(packageId: String): String =
      """
        package $packageId.ui.transform
        
        import android.os.Bundle
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import android.widget.ImageView
        import android.widget.TextView
        import androidx.core.content.res.ResourcesCompat
        import androidx.fragment.app.Fragment
        import androidx.lifecycle.ViewModelProvider
        import androidx.recyclerview.widget.DiffUtil
        import androidx.recyclerview.widget.ListAdapter
        import androidx.recyclerview.widget.RecyclerView
        import $packageId.R
        import $packageId.databinding.FragmentTransformBinding
        import $packageId.databinding.ItemTransformBinding
        
        /**
         * Fragment that demonstrates a responsive layout pattern where the format of the content
         * transforms depending on the size of the screen. Specifically this Fragment shows items in
         * the [RecyclerView] using LinearLayoutManager in a small screen
         * and shows items using GridLayoutManager in a large screen.
         */
        class TransformFragment : Fragment() {
        
            private var _binding: FragmentTransformBinding? = null
        
            // This property is only valid between onCreateView and
            // onDestroyView.
            private val binding get() = _binding!!
        
            override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View {
                val transformViewModel = ViewModelProvider(this).get(TransformViewModel::class.java)
                _binding = FragmentTransformBinding.inflate(inflater, container, false)
                val root: View = binding.root
        
                val recyclerView = binding.recyclerviewTransform
                val adapter = TransformAdapter()
                recyclerView.adapter = adapter
                transformViewModel.texts.observe(viewLifecycleOwner) {
                    adapter.submitList(it)
                }
                return root
            }
        
            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }
        
            class TransformAdapter :
                ListAdapter<String, TransformViewHolder>(object : DiffUtil.ItemCallback<String>() {
        
                    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                        oldItem == newItem
        
                    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                        oldItem == newItem
                }) {
        
                private val drawables = listOf(
                    R.drawable.avatar_1,
                    R.drawable.avatar_2,
                    R.drawable.avatar_3,
                    R.drawable.avatar_4,
                    R.drawable.avatar_5,
                    R.drawable.avatar_6,
                    R.drawable.avatar_7,
                    R.drawable.avatar_8,
                    R.drawable.avatar_9,
                    R.drawable.avatar_10,
                    R.drawable.avatar_11,
                    R.drawable.avatar_12,
                    R.drawable.avatar_13,
                    R.drawable.avatar_14,
                    R.drawable.avatar_15,
                    R.drawable.avatar_16,
                )
        
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
                    val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context))
                    return TransformViewHolder(binding)
                }
        
                override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
                    holder.textView.text = getItem(position)
                    holder.imageView.setImageDrawable(
                        ResourcesCompat.getDrawable(holder.imageView.resources, drawables[position], null)
                    )
                }
            }
        
            class TransformViewHolder(binding: ItemTransformBinding) :
                RecyclerView.ViewHolder(binding.root) {
        
                val imageView: ImageView = binding.imageViewItemTransform
                val textView: TextView = binding.textViewItemTransform
            }
        }
    """
          .trimIndent()

  fun templateTypeTransformJava(packageId: String): String =
      """
        package $packageId.ui.transform;
        
        import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ImageView;
        import android.widget.TextView;
        
        import androidx.annotation.NonNull;
        import androidx.core.content.res.ResourcesCompat;
        import androidx.fragment.app.Fragment;
        import androidx.lifecycle.ViewModelProvider;
        import androidx.recyclerview.widget.DiffUtil;
        import androidx.recyclerview.widget.ListAdapter;
        import androidx.recyclerview.widget.RecyclerView;
        
        import $packageId.R;
        import $packageId.databinding.FragmentTransformBinding;
        import $packageId.databinding.ItemTransformBinding;
        
        import java.util.Arrays;
        import java.util.List;
        
        /**
         * Fragment that demonstrates a responsive layout pattern where the format of the content
         * transforms depending on the size of the screen. Specifically this Fragment shows items in
         * the [RecyclerView] using LinearLayoutManager in a small screen
         * and shows items using GridLayoutManager in a large screen.
         */
        public class TransformFragment extends Fragment {
        
            private FragmentTransformBinding binding;
        
            public View onCreateView(@NonNull LayoutInflater inflater,
                                     ViewGroup container, Bundle savedInstanceState) {
                TransformViewModel transformViewModel =
                        new ViewModelProvider(this).get(TransformViewModel.class);
        
                binding = FragmentTransformBinding.inflate(inflater, container, false);
                View root = binding.getRoot();
        
                RecyclerView recyclerView = binding.recyclerviewTransform;
                ListAdapter<String, TransformViewHolder> adapter = new TransformAdapter();
                recyclerView.setAdapter(adapter);
                transformViewModel.getTexts().observe(getViewLifecycleOwner(), adapter::submitList);
                return root;
            }
        
            @Override
            public void onDestroyView() {
                super.onDestroyView();
                binding = null;
            }
        
            private static class TransformAdapter extends ListAdapter<String, TransformViewHolder> {
        
                private final List<Integer> drawables = Arrays.asList(
                        R.drawable.avatar_1,
                        R.drawable.avatar_2,
                        R.drawable.avatar_3,
                        R.drawable.avatar_4,
                        R.drawable.avatar_5,
                        R.drawable.avatar_6,
                        R.drawable.avatar_7,
                        R.drawable.avatar_8,
                        R.drawable.avatar_9,
                        R.drawable.avatar_10,
                        R.drawable.avatar_11,
                        R.drawable.avatar_12,
                        R.drawable.avatar_13,
                        R.drawable.avatar_14,
                        R.drawable.avatar_15,
                        R.drawable.avatar_16);
        
                protected TransformAdapter() {
                    super(new DiffUtil.ItemCallback<String>() {
                        @Override
                        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                            return oldItem.equals(newItem);
                        }
        
                        @Override
                        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                            return oldItem.equals(newItem);
                        }
                    });
                }
        
                @NonNull
                @Override
                public TransformViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    ItemTransformBinding binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.getContext()));
                    return new TransformViewHolder(binding);
                }
        
                @Override
                public void onBindViewHolder(@NonNull TransformViewHolder holder, int position) {
                    holder.textView.setText(getItem(position));
                    holder.imageView.setImageDrawable(
                            ResourcesCompat.getDrawable(holder.imageView.getResources(),
                                    drawables.get(position),
                                    null));
                }
            }
        
            private static class TransformViewHolder extends RecyclerView.ViewHolder {
        
                private final ImageView imageView;
                private final TextView textView;
        
                public TransformViewHolder(ItemTransformBinding binding) {
                    super(binding.getRoot());
                    imageView = binding.imageViewItemTransform;
                    textView = binding.textViewItemTransform;
                }
            }
        }
    """
          .trimIndent()

  fun templateTypeKt(packageId: String, fragmentName: String): String {
    val nameCap = fragmentName.replaceFirstChar { it.uppercase() }
    val bindingName = "Fragment${nameCap}Binding"
    val textId = "text${nameCap}"

    return """
            package $packageId.ui.$fragmentName

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import android.widget.TextView
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.ViewModelProvider
            import $packageId.databinding.$bindingName

            class ${nameCap}Fragment : Fragment() {

                private var _binding: $bindingName? = null
                private val binding get() = _binding!!

                override fun onCreateView(
                    inflater: LayoutInflater,
                    container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View {
                    val ${fragmentName}ViewModel =
                        ViewModelProvider(this).get(${nameCap}ViewModel::class.java)

                    _binding = $bindingName.inflate(inflater, container, false)
                    val root: View = binding.root

                    val textView: TextView = binding.$textId
                    ${fragmentName}ViewModel.text.observe(viewLifecycleOwner) {
                        textView.text = it
                    }
                    return root
                }

                override fun onDestroyView() {
                    super.onDestroyView()
                    _binding = null
                }
            }
        """
        .trimIndent()
  }

  fun templateTypeJava(packageId: String, fragmentName: String): String {
    val nameCap = fragmentName.replaceFirstChar { it.uppercase() }
    val bindingName = "Fragment${nameCap}Binding"
    val viewModelName = "${nameCap}ViewModel"
    val textId = "text${nameCap}"

    return """
            package $packageId.ui.$fragmentName;
            
            import android.os.Bundle;
            import android.view.LayoutInflater;
            import android.view.View;
            import android.view.ViewGroup;
            import android.widget.TextView;
            
            import androidx.annotation.NonNull;
            import androidx.fragment.app.Fragment;
            import androidx.lifecycle.ViewModelProvider;
            
            import $packageId.databinding.$bindingName;
            
            public class ${nameCap}Fragment extends Fragment {
            
                private $bindingName binding;
            
                @Override
                public View onCreateView(@NonNull LayoutInflater inflater,
                                         ViewGroup container, Bundle savedInstanceState) {
                    $viewModelName ${fragmentName}ViewModel =
                            new ViewModelProvider(this).get($viewModelName.class);
            
                    binding = $bindingName.inflate(inflater, container, false);
                    View root = binding.getRoot();
            
                    final TextView textView = binding.$textId;
                    ${fragmentName}ViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
                    return root;
                }
            
                @Override
                public void onDestroyView() {
                    super.onDestroyView();
                    binding = null;
                }
            }
        """
        .trimIndent()
  }
}

object ViewModel {

  fun templateTypeTransformKt(packageId: String): String =
      """
        package $packageId.ui.transform
        
        import androidx.lifecycle.LiveData
        import androidx.lifecycle.MutableLiveData
        import androidx.lifecycle.ViewModel
        
        class TransformViewModel : ViewModel() {
        
            private val _texts = MutableLiveData<List<String>>().apply {
                value = (1..16).mapIndexed { _, i ->
                    "This is item # ${'$'}i"
                }
            }
        
            val texts: LiveData<List<String>> = _texts
        }
    """
          .trimIndent()

  fun templateTypeTransformJava(packageId: String): String =
      """
        package $packageId.ui.transform;
        
        import androidx.lifecycle.LiveData;
        import androidx.lifecycle.MutableLiveData;
        import androidx.lifecycle.ViewModel;
        
        import java.util.ArrayList;
        import java.util.List;
        
        public class TransformViewModel extends ViewModel {
        
            private final MutableLiveData<List<String>> mTexts;
        
            public TransformViewModel() {
                mTexts = new MutableLiveData<>();
                List<String> texts = new ArrayList<>();
                for (int i = 1; i <= 16; i++) {
                    texts.add("This is item # " + i);
                }
                mTexts.setValue(texts);
            }
        
            public LiveData<List<String>> getTexts() {
                return mTexts;
            }
        }
    """
          .trimIndent()

  fun templateTypeKt(packageId: String, className: String): String =
      """
        package $packageId.ui.$className

        import androidx.lifecycle.LiveData
        import androidx.lifecycle.MutableLiveData
        import androidx.lifecycle.ViewModel

        class ${className.replaceFirstChar { it.uppercase() }}ViewModel : ViewModel() {

            private val _text = MutableLiveData<String>().apply {
                value = "This is ${className.replaceFirstChar { it.uppercase() }} Fragment"
            }
            val text: LiveData<String> = _text
        }
    """
          .trimIndent()

  fun templateTypeJava(packageId: String, className: String): String =
      """
        package $packageId.ui.$className;
        
        import androidx.lifecycle.LiveData;
        import androidx.lifecycle.MutableLiveData;
        import androidx.lifecycle.ViewModel;
        
        public class ${className.replaceFirstChar { it.uppercase() }}ViewModel extends ViewModel {
        
            private final MutableLiveData<String> mText;
        
            public ${className.replaceFirstChar { it.uppercase() }}ViewModel() {
                mText = new MutableLiveData<>();
                mText.setValue("This is ${className.replaceFirstChar { it.uppercase() }} fragment");
            }
        
            public LiveData<String> getText() {
                return mText;
            }
        }
    """
          .trimIndent()
}
