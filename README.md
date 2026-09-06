abu_hurayra_hadith/
│
├── android/
├── assets/
│   └── data/
│       └── hadiths.json
│
├── lib/
│   ├── main.dart
│   │
│   ├── models/
│   │   └── hadith.dart
│   │
│   ├── services/
│   │   └── storage_service.dart
│   │
│   ├── data/
│   │   └── hadith_data.dart
│   │
│   ├── screens/
│   │   ├── home_screen.dart
│   │   ├── hadith_detail_screen.dart
│   │   ├── favorites_screen.dart
│   │   └── about_screen.dart
│   │
│   └── widgets/
│       └── hadith_card.dart
│
└── pubspec.yamlname: abu_hurayra_hadith
description: App Af Soomaali ah oo loogu talagalay Xadiisyada Abu Hurayra RC.
publish_to: "none"

version: 1.0.0+1

environment:
  sdk: ">=3.0.0 <4.0.0"

dependencies:
  flutter:
    sdk: flutter

  cupertino_icons: ^1.0.8
  shared_preferences: ^2.2.3

dev_dependencies:
  flutter_test:
    sdk: flutter

  flutter_lints: ^5.0.0

flutter:
  uses-material-design: true

  assets:
    - assets/data/hadiths.jsonclass Hadith {
  final int id;
  final String title;
  final String arabic;
  final String somali;
  final String source;
  final String category;

  const Hadith({
    required this.id,
    required this.title,
    required this.arabic,
    required this.somali,
    required this.source,
    required this.category,
  });

  factory Hadith.fromJson(Map<String, dynamic> json) {
    return Hadith(
      id: json['id'] ?? 0,
      title: json['title'] ?? '',
      arabic: json['arabic'] ?? '',
      somali: json['somali'] ?? '',
      source: json['source'] ?? '',
      category: json['category'] ?? '',
    );
  }
}[
  {
    "id": 1,
    "title": "Xadiiska 1",
    "arabic": "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الْآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
    "somali": "Qofkii rumaysan Allah iyo Maalinta Aakhiro ha ku hadlo wanaag ama ha aamuso.",
    "source": "Waxaa weriyey Abu Hurayra (RC)",
    "category": "Akhlaaq"
  },
  {
    "id": 2,
    "title": "Xadiiska 2",
    "arabic": "لَا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لِأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
    "somali": "Midkiin rumayntiisu ma dhammaato ilaa uu walaalkiis Muslimka ah u jeclaado waxa uu naftiisa u jecel yahay.",
    "source": "Waxaa weriyey Abu Hurayra (RC)",
    "category": "Iimaan"
  },
  {
    "id": 3,
    "title": "Xadiiska 3",
    "arabic": "مَنْ نَفَّسَ عَنْ مُؤْمِنٍ كُرْبَةً مِنْ كُرَبِ الدُّنْيَا نَفَّسَ اللَّهُ عَنْهُ كُرْبَةً مِنْ كُرَبِ يَوْمِ الْقِيَامَةِ",
    "somali": "Qofkii ka fayda Mu'min dhib ka mid ah dhibaatooyinka adduunka, Allah wuxuu ka faydi doonaa dhib ka mid ah dhibaatooyinka Maalinta Qiyaamaha.",
    "source": "Waxaa weriyey Abu Hurayra (RC)",
    "category": "Caawinta"
  },
  {
    "id": 4,
    "title": "Xadiiska 4",
    "arabic": "الطُّهُورُ شَطْرُ الْإِيمَانِ",
    "somali": "Daahirnimadu waa qayb weyn oo iimaanka ka mid ah.",
    "source": "Waxaa weriyey Abu Hurayra (RC)",
    "category": "Daahirnimo"
  },
  {
    "id": 5,
    "title": "Xadiiska 5",
    "arabic": "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
    "somali": "Qofkii mara waddo uu cilmi ku raadinayo, Allah wuxuu u fududaynayaa waddada Jannada.",
    "source": "Waxaa weriyey Abu Hurayra (RC)",
    "category": "Cilmi"
  }
]import 'dart:convert';
import 'package:flutter/services.dart';
import '../models/hadith.dart';

class HadithData {
  static Future<List<Hadith>> loadHadiths() async {
    final String response =
        await rootBundle.loadString('assets/data/hadiths.json');

    final List<dynamic> data = json.decode(response);

    return data
        .map((item) => Hadith.fromJson(item))
        .toList();
  }
}import 'package:shared_preferences/shared_preferences.dart';

class StorageService {
  static const String _favoritesKey = 'favorite_hadiths';

  static Future<List<int>> getFavorites() async {
    final prefs = await SharedPreferences.getInstance();

    final list = prefs.getStringList(_favoritesKey) ?? [];

    return list.map((item) => int.parse(item)).toList();
  }

  static Future<void> saveFavorites(List<int> ids) async {
    final prefs = await SharedPreferences.getInstance();

    await prefs.setStringList(
      _favoritesKey,
      ids.map((id) => id.toString()).toList(),
    );
  }

  static Future<void> toggleFavorite(
    int id,
    List<int> favorites,
  ) async {
    if (favorites.contains(id)) {
      favorites.remove(id);
    } else {
      favorites.add(id);
    }

    await saveFavorites(favorites);
  }
}import 'package:flutter/material.dart';
import '../models/hadith.dart';

class HadithCard extends StatelessWidget {
  final Hadith hadith;
  final bool isFavorite;
  final VoidCallback onTap;
  final VoidCallback onFavorite;

  const HadithCard({
    super.key,
    required this.hadith,
    required this.isFavorite,
    required this.onTap,
    required this.onFavorite,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 3,
      margin: const EdgeInsets.symmetric(
        horizontal: 14,
        vertical: 8,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [

              Row(
                children: [
                  CircleAvatar(
                    backgroundColor: Colors.blue.shade800,
                    foregroundColor: Colors.white,
                    child: Text('${hadith.id}'),
                  ),

                  const SizedBox(width: 12),

                  Expanded(
                    child: Text(
                      hadith.title,
                      style: const TextStyle(
                        fontSize: 19,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),

                  IconButton(
                    onPressed: onFavorite,
                    icon: Icon(
                      isFavorite
                          ? Icons.bookmark
                          : Icons.bookmark_border,
                      color: Colors.amber.shade700,
                    ),
                  ),
                ],
              ),

              const SizedBox(height: 15),

              Directionality(
                textDirection: TextDirection.rtl,
                child: Text(
                  hadith.arabic,
                  textAlign: TextAlign.right,
                  style: const TextStyle(
                    fontSize: 21,
                    height: 1.8,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),

              const SizedBox(height: 15),

              Text(
                hadith.somali,
                style: const TextStyle(
                  fontSize: 16,
                  height: 1.6,
                ),
              ),

              const SizedBox(height: 12),

              Chip(
                label: Text(hadith.category),
                avatar: const Icon(
                  Icons.category,
                  size: 18,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}import 'package:flutter/material.dart';
import '../models/hadith.dart';

class HadithDetailScreen extends StatelessWidget {
  final Hadith hadith;
  final bool isFavorite;
  final VoidCallback onFavorite;

  const HadithDetailScreen({
    super.key,
    required this.hadith,
    required this.isFavorite,
    required this.onFavorite,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(hadith.title),
        actions: [
          IconButton(
            onPressed: onFavorite,
            icon: Icon(
              isFavorite
                  ? Icons.bookmark
                  : Icons.bookmark_border,
            ),
          ),
        ],
      ),

      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [

            Center(
              child: CircleAvatar(
                radius: 40,
                backgroundColor: Colors.blue.shade800,
                foregroundColor: Colors.white,
                child: const Icon(
                  Icons.menu_book,
                  size: 40,
                ),
              ),
            ),

            const SizedBox(height: 25),

            const Text(
              '📖 Xadiiska Carabiga',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 12),

            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Directionality(
                  textDirection: TextDirection.rtl,
                  child: Text(
                    hadith.arabic,
                    textAlign: TextAlign.right,
                    style: const TextStyle(
                      fontSize: 25,
                      height: 2,
                    ),
                  ),
                ),
              ),
            ),

            const SizedBox(height: 25),

            const Text(
              '🇸🇴 Tarjumaadda Af-Soomaaliga',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 12),

            Text(
              hadith.somali,
              style: const TextStyle(
                fontSize: 18,
                height: 1.8,
              ),
            ),

            const SizedBox(height: 30),

            Card(
              color: Colors.blue.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline),

                    const SizedBox(width: 12),

                    Expanded(
                      child: Text(
                        hadith.source,
                        style: const TextStyle(
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 25),

            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: onFavorite,
                icon: Icon(
                  isFavorite
                      ? Icons.bookmark
                      : Icons.bookmark_add_outlined,
                ),
                label: Text(
                  isFavorite
                      ? 'Ka saar Kaydka'
                      : 'Ku dar Kaydka',
                ),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.all(16),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}import 'package:flutter/material.dart';
import '../models/hadith.dart';
import '../widgets/hadith_card.dart';

class FavoritesScreen extends StatelessWidget {
  final List<Hadith> favorites;
  final List<int> favoriteIds;
  final Function(Hadith) onOpen;
  final Function(int) onFavorite;

  const FavoritesScreen({
    super.key,
    required this.favorites,
    required this.favoriteIds,
    required this.onOpen,
    required this.onFavorite,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('⭐ Xadiisyada Kaydsan'),
      ),
      body: favorites.isEmpty
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.bookmark_border,
                    size: 80,
                  ),
                  SizedBox(height: 15),
                  Text(
                    'Weli xadiis ma kaydsan',
                    style: TextStyle(fontSize: 18),
                  ),
                ],
              ),
            )
          : ListView.builder(
              itemCount: favorites.length,
              itemBuilder: (context, index) {
                final hadith = favorites[index];

                return HadithCard(
                  hadith: hadith,
                  isFavorite: favoriteIds.contains(hadith.id),
                  onTap: () => onOpen(hadith),
                  onFavorite: () => onFavorite(hadith.id),
                );
              },
            ),
    );
  }
}import 'package:flutter/material.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ku Saabsan App-ka'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [

            const SizedBox(height: 30),

            CircleAvatar(
              radius: 60,
              backgroundColor: Colors.blue.shade800,
              foregroundColor: Colors.white,
              child: const Icon(
                Icons.menu_book,
                size: 65,
              ),
            ),

            const SizedBox(height: 25),

            const Text(
              'Xadiisyadii Abu Hurayra (RC)',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),

            const SizedBox(height: 20),

            const Text(
              'App-kan waxaa loogu talagalay in lagu akhriyo laguna barto xadiisyada iyo tarjumaaddooda Af-Soomaaliga.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 17,
                height: 1.6,
              ),
            ),

            const SizedBox(height: 30),

            const Text(
              '📖 Carabi\n🇸🇴 Af-Soomaali\n⭐ Kaydinta Xadiisyada\n🔍 Raadinta',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 17,
                height: 1.8,
              ),
            ),
          ],
        ),
      ),
    );
  }
}import 'package:flutter/material.dart';

import '../models/hadith.dart';
import '../data/hadith_data.dart';
import '../services/storage_service.dart';
import '../widgets/hadith_card.dart';

import 'hadith_detail_screen.dart';
import 'favorites_screen.dart';
import 'about_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {

  List<Hadith> hadiths = [];
  List<int> favorites = [];

  String searchText = '';

  bool loading = true;

  @override
  void initState() {
    super.initState();
    loadData();
  }

  Future<void> loadData() async {
    final loadedHadiths =
        await HadithData.loadHadiths();

    final loadedFavorites =
        await StorageService.getFavorites();

    setState(() {
      hadiths = loadedHadiths;
      favorites = loadedFavorites;
      loading = false;
    });
  }

  Future<void> toggleFavorite(int id) async {
    await StorageService.toggleFavorite(
      id,
      favorites,
    );

    setState(() {});
  }

  void openHadith(Hadith hadith) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => HadithDetailScreen(
          hadith: hadith,
          isFavorite: favorites.contains(hadith.id),
          onFavorite: () async {
            await toggleFavorite(hadith.id);

            if (mounted) {
              Navigator.pop(context);
            }
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {

    final filteredHadiths =
        hadiths.where((hadith) {

      final query = searchText.toLowerCase();

      return hadith.title
              .toLowerCase()
              .contains(query) ||

          hadith.somali
              .toLowerCase()
              .contains(query) ||

          hadith.category
              .toLowerCase()
              .contains(query) ||

          hadith.arabic.contains(searchText);

    }).toList();

    return Scaffold(

      appBar: AppBar(
        title: const Text(
          '📖 Xadiisyadii Abu Hurayra (RC)',
        ),

        actions: [

          IconButton(
            icon: const Icon(Icons.info_outline),

            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      const AboutScreen(),
                ),
              );
            },
          ),

        ],
      ),

      drawer: Drawer(
        child: ListView(
          padding: EdgeInsets.zero,

          children: [

            DrawerHeader(
              decoration: BoxDecoration(
                color: Colors.blue.shade800,
              ),

              child: const Column(
                crossAxisAlignment:
                    CrossAxisAlignment.start,

                children: [

                  Icon(
                    Icons.menu_book,
                    color: Colors.white,
                    size: 50,
                  ),

                  SizedBox(height: 15),

                  Text(
                    'Xadiisyadii\nAbu Hurayra (RC)',

                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight:
                          FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),

            ListTile(
              leading: const Icon(Icons.home),

              title: const Text('Bogga Hore'),

              onTap: () {
                Navigator.pop(context);
              },
            ),

            ListTile(
              leading:
                  const Icon(Icons.bookmark),

              title:
                  const Text('Xadiisyada Kaydsan'),

              onTap: () {

                Navigator.pop(context);

                final favoriteHadiths =
                    hadiths.where((hadith) {

                  return favorites.contains(
                    hadith.id,
                  );

                }).toList();

                Navigator.push(
                  context,

                  MaterialPageRoute(
                    builder: (context) =>
                        FavoritesScreen(

                      favorites:
                          favoriteHadiths,

                      favoriteIds:
                          favorites,

                      onOpen: openHadith,

                      onFavorite: toggleFavorite,
                    ),
                  ),
                ).then((_) {
                  setState(() {});
                });

              },
            ),

            const Divider(),

            ListTile(
              leading:
                  const Icon(Icons.info),

              title:
                  const Text('Ku Saabsan App-ka'),

              onTap: () {

                Navigator.push(
                  context,

                  MaterialPageRoute(
                    builder: (context) =>
                        const AboutScreen(),
                  ),
                );

              },
            ),
          ],
        ),
      ),

      body: loading

          ? const Center(
              child:
                  CircularProgressIndicator(),
            )

          : Column(
              children: [

                Container(
                  padding:
                      const EdgeInsets.all(16),

                  child: TextField(

                    onChanged: (value) {

                      setState(() {
                        searchText = value;
                      });

                    },

                    decoration: InputDecoration(

                      hintText:
                          '🔍 Raadi xadiis...',

                      prefixIcon:
                          const Icon(Icons.search),

                      filled: true,

                      border:
                          OutlineInputBorder(

                        borderRadius:
                            BorderRadius.circular(18),

                      ),
                    ),
                  ),
                ),

                Expanded(

                  child: filteredHadiths.isEmpty

                      ? const Center(
                          child: Text(
                            'Xadiis lama helin',
                            style: TextStyle(
                              fontSize: 18,
                            ),
                          ),
                        )

                      : ListView.builder(

                          itemCount:
                              filteredHadiths.length,

                          itemBuilder:
                              (context, index) {

                            final hadith =
                                filteredHadiths[index];

                            return HadithCard(

                              hadith: hadith,

                              isFavorite:
                                  favorites.contains(
                                hadith.id,
                              ),

                              onTap: () =>
                                  openHadith(hadith),

                              onFavorite: () =>
                                  toggleFavorite(
                                hadith.id,
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
    );
  }
}import 'package:flutter/material.dart';

import 'screens/home_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const AbuHurayraApp());
}

class AbuHurayraApp extends StatelessWidget {
  const AbuHurayraApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,

      title: 'Xadiisyadii Abu Hurayra RC',

      theme: ThemeData(
        useMaterial3: true,

        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue.shade800,
        ),

        appBarTheme: AppBarTheme(
          backgroundColor: Colors.blue.shade800,
          foregroundColor: Colors.white,
          centerTitle: true,
          elevation: 2,
        ),

        cardTheme: CardThemeData(
          elevation: 3,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
        ),
      ),

      home: const HomeScreen(),
    );
  }
}
