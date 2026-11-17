part of '../main.dart';

class _LogoMark extends StatelessWidget {
  const _LogoMark({this.size = 38});
  final double size;
  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      border: Border.all(color: _brick, width: 1.4),
      color: Colors.black,
    ),
    clipBehavior: Clip.antiAlias,
    child: Image.asset(
      'assets/zakzouka_logo.jpg',
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) =>
          const Icon(Icons.movie, color: Colors.white, size: 20),
    ),
  );
}

class _FeaturedCarousel extends StatefulWidget {
  const _FeaturedCarousel({required this.items, required this.onOpen});
  final List<MediaItem> items;
  final ValueChanged<MediaItem> onOpen;
  @override
  State<_FeaturedCarousel> createState() => _FeaturedCarouselState();
}

class _FeaturedCarouselState extends State<_FeaturedCarousel> {
  late final PageController _controller = PageController(viewportFraction: .94);
  int _current = 0;

  @override
  void didUpdateWidget(covariant _FeaturedCarousel oldWidget) {
    super.didUpdateWidget(oldWidget);
    final changedItems =
        oldWidget.items.length != widget.items.length ||
        (oldWidget.items.isNotEmpty &&
            widget.items.isNotEmpty &&
            oldWidget.items.first.pageUrl != widget.items.first.pageUrl);
    if (!changedItems) return;
    _current = 0;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _controller.hasClients) _controller.jumpToPage(0);
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      SizedBox(
        height: 208,
        child: PageView.builder(
          controller: _controller,
          itemCount: widget.items.length,
          onPageChanged: (index) => setState(() => _current = index),
          itemBuilder: (_, index) => Padding(
            padding: const EdgeInsetsDirectional.only(start: 2, end: 8),
            child: _FeaturedCard(
              item: widget.items[index],
              onTap: () => widget.onOpen(widget.items[index]),
            ),
          ),
        ),
      ),
      if (widget.items.length > 1) ...[
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(widget.items.length, (index) {
            final active = index == _current;
            return AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              margin: const EdgeInsets.symmetric(horizontal: 3),
              width: active ? 18 : 6,
              height: 6,
              decoration: BoxDecoration(
                color: active ? _brick : _line,
                borderRadius: BorderRadius.circular(8),
              ),
            );
          }),
        ),
      ],
    ],
  );
}

class _FeaturedCard extends StatelessWidget {
  const _FeaturedCard({required this.item, required this.onTap});
  final MediaItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final title = item.title.trim();
    // Dynamic adaptive font size: ensures complete title fits without awkward cuts
    final double titleFontSize = title.length > 38
        ? 14.5
        : (title.length > 24 ? 16.5 : 19.0);

    return Material(
      color: const Color(0xFF3C2D3A),
      borderRadius: BorderRadius.circular(24),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Container(
          height: 198,
          decoration: const BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.centerRight,
              end: Alignment.centerLeft,
              colors: [Color(0xFF30232E), Color(0xFF594653), Color(0xFF8A777A)],
            ),
          ),
          child: Stack(
            children: [
              Positioned(
                left: 8,
                top: 9,
                bottom: 9,
                width: 118,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: _NetworkImage(url: item.poster, fit: BoxFit.contain),
                ),
              ),
              Positioned(
                left: 132,
                right: 14,
                top: 10,
                bottom: 10,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text(
                      'اختيار اليوم',
                      textAlign: TextAlign.right,
                      style: TextStyle(
                        color: Colors.white70,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      title,
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                      textDirection: TextDirection.rtl,
                      textAlign: TextAlign.right,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: titleFontSize,
                        fontWeight: FontWeight.w900,
                        height: 1.24,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: [
                        const Icon(
                          Icons.play_circle_fill,
                          size: 30,
                          color: Colors.white,
                        ),
                        const SizedBox(width: 8),
                        _SmallBadge(label: item.isSeries ? 'مسلسل' : 'فيلم'),
                        if (item.year.isNotEmpty) ...[
                          const SizedBox(width: 6),
                          _SmallBadge(label: item.year),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SmallBadge extends StatelessWidget {
  const _SmallBadge({required this.label});
  final String label;
  @override
  Widget build(BuildContext context) => DecoratedBox(
    decoration: BoxDecoration(
      color: Colors.black.withOpacity(.58),
      borderRadius: BorderRadius.circular(8),
    ),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Text(
        label,
        style: const TextStyle(color: Colors.white, fontSize: 10),
      ),
    ),
  );
}

class MediaPoster extends StatelessWidget {
  const MediaPoster({
    super.key,
    required this.item,
    required this.favorite,
    required this.onTap,
    required this.onFavorite,
  });
  final MediaItem item;
  final bool favorite;
  final VoidCallback onTap, onFavorite;
  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(15),
    onTap: onTap,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: Stack(
            fit: StackFit.expand,
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(15),
                child: _NetworkImage(url: item.poster, fit: BoxFit.cover),
              ),
              Positioned(
                right: 6,
                top: 6,
                child: _SmallBadge(label: item.isSeries ? 'مسلسل' : 'فيلم'),
              ),
              if (item.quality.isNotEmpty)
                Positioned(
                  left: 5,
                  top: 5,
                  child: _SmallBadge(label: item.quality),
                ),
              Positioned(
                left: 0,
                bottom: 0,
                child: IconButton(
                  visualDensity: VisualDensity.compact,
                  onPressed: onFavorite,
                  icon: Icon(
                    favorite ? Icons.favorite : Icons.favorite_border,
                    color: favorite ? _brick : Colors.white,
                    size: 19,
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 7),
        Text(
          item.title,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: _ink,
            fontSize: 12,
            height: 1.35,
            fontWeight: FontWeight.w700,
          ),
        ),
        if (item.year.isNotEmpty || item.latestEpisodeLabel.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 3),
            child: Text(
              item.latestEpisodeLabel.isNotEmpty
                  ? item.latestEpisodeLabel
                  : '${item.year} · عام',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, fontSize: 10),
            ),
          ),
      ],
    ),
  );
}

class _NetworkImage extends StatelessWidget {
  const _NetworkImage({required this.url, this.fit = BoxFit.cover});
  final String url;
  final BoxFit fit;
  @override
  Widget build(BuildContext context) {
    if (url.isEmpty) return const _PosterSkeleton();
    return CachedNetworkImage(
      imageUrl: url,
      httpHeaders: _posterHeaders(url),
      fit: fit,
      memCacheWidth: 500,
      memCacheHeight: 900,
      maxWidthDiskCache: 650,
      maxHeightDiskCache: 1100,
      filterQuality: FilterQuality.low,
      fadeInDuration: const Duration(milliseconds: 120),
      useOldImageOnUrlChange: true,
      placeholder: (_, __) => const _PosterSkeleton(),
      errorWidget: (_, __, ___) => const _PosterFallback(),
    );
  }
}

Map<String, String> _posterHeaders(String rawUrl) {
  final uri = Uri.tryParse(rawUrl);
  final host = uri?.host.toLowerCase() ?? '';
  final referer = switch (host) {
    _ when host.contains('myseed') => 'https://m.myseed.tv/',
    _ when host.contains('arabseed') => 'https://arabseed.health/',
    _ when host.contains('akw') => 'https://akw.cam/',
    _ when host.contains('wecima') => 'https://myweecima.casa/',
    _ when host.contains('fasel') => 'https://wb49584.fasel4k.site/',
    _ when host.isNotEmpty =>
      '${uri!.scheme.isEmpty ? 'https' : uri.scheme}://$host/',
    _ => '',
  };
  return {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36',
    if (referer.isNotEmpty) 'Referer': referer,
    'Accept': 'image/avif,image/webp,image/apng,image/*,*/*;q=0.8',
  };
}

class _PosterFallback extends StatelessWidget {
  const _PosterFallback();
  @override
  Widget build(BuildContext context) => Container(
    color: const Color(0xFFE8DBCC),
    alignment: Alignment.center,
    child: const Icon(Icons.movie_outlined, color: _muted, size: 28),
  );
}

class _PosterSkeleton extends StatelessWidget {
  const _PosterSkeleton();
  @override
  Widget build(BuildContext context) => Container(
    decoration: BoxDecoration(
      color: const Color(0xFFEADBCB),
      borderRadius: BorderRadius.circular(15),
    ),
  );
}

class _CatalogSkeleton extends StatelessWidget {
  const _CatalogSkeleton();
  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
      children: [
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: _SkeletonBlock(width: 142, height: 9),
        ),
        const SizedBox(height: 12),
        const _SkeletonBlock(height: 190, radius: 24),
        const SizedBox(height: 18),
        Align(
          alignment: AlignmentDirectional.centerEnd,
          child: _SkeletonBlock(width: 124, height: 9),
        ),
        const SizedBox(height: 12),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: 6,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            crossAxisSpacing: 10,
            mainAxisSpacing: 14,
            childAspectRatio: .50,
          ),
          itemBuilder: (_, __) => const _CatalogPosterSkeleton(),
        ),
      ],
    ),
  );
}

class _CatalogPosterSkeleton extends StatelessWidget {
  const _CatalogPosterSkeleton();

  @override
  Widget build(BuildContext context) => Column(
    children: [
      const Expanded(child: _SkeletonBlock(radius: 15)),
      const SizedBox(height: 7),
      const _SkeletonBlock(width: double.infinity, height: 8, radius: 5),
      const SizedBox(height: 5),
      Align(
        alignment: AlignmentDirectional.centerEnd,
        child: _SkeletonBlock(
          width: MediaQuery.sizeOf(context).width * .16,
          height: 7,
          radius: 5,
        ),
      ),
    ],
  );
}

class _SportsMatchSkeleton extends StatelessWidget {
  const _SportsMatchSkeleton();

  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: 3,
      itemBuilder: (_, __) => Container(
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _card,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: _line),
        ),
        child: Column(
          children: [
            const Align(
              alignment: AlignmentDirectional.centerEnd,
              child: _SkeletonBlock(width: 112, height: 8),
            ),
            const SizedBox(height: 14),
            Row(
              children: [
                const Expanded(child: _SkeletonTeam()),
                const _SkeletonBlock(width: 64, height: 31, radius: 9),
                const Expanded(child: _SkeletonTeam()),
              ],
            ),
            const SizedBox(height: 14),
            const _SkeletonBlock(width: double.infinity, height: 8, radius: 5),
          ],
        ),
      ),
    ),
  );
}

class _SkeletonTeam extends StatelessWidget {
  const _SkeletonTeam();

  @override
  Widget build(BuildContext context) => Column(
    children: [
      const _SkeletonBlock(width: 42, height: 42, radius: 50),
      const SizedBox(height: 7),
      _SkeletonBlock(width: MediaQuery.sizeOf(context).width * .14, height: 8),
    ],
  );
}

class _SportsChannelSkeleton extends StatelessWidget {
  const _SportsChannelSkeleton();

  @override
  Widget build(BuildContext context) => _ShimmerLoading(
    child: ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(14),
      itemCount: 5,
      separatorBuilder: (_, __) => const SizedBox(height: 9),
      itemBuilder: (_, __) => Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: _card,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _line),
        ),
        child: Row(
          children: [
            const _SkeletonBlock(width: 42, height: 42, radius: 50),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const _SkeletonBlock(width: 112, height: 9),
                  const SizedBox(height: 8),
                  _SkeletonBlock(
                    width: MediaQuery.sizeOf(context).width * .34,
                    height: 7,
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            const _SkeletonBlock(width: 22, height: 22, radius: 50),
          ],
        ),
      ),
    ),
  );
}

class _SkeletonBlock extends StatelessWidget {
  const _SkeletonBlock({this.width, this.height, this.radius = 9});

  final double? width;
  final double? height;
  final double radius;

  @override
  Widget build(BuildContext context) => Container(
    width: width,
    height: height,
    decoration: BoxDecoration(
      color: const Color(0xFFEADBCB),
      borderRadius: BorderRadius.circular(radius),
    ),
  );
}

class _ShimmerLoading extends StatefulWidget {
  const _ShimmerLoading({required this.child});
  final Widget child;

  @override
  State<_ShimmerLoading> createState() => _ShimmerLoadingState();
}

class _ShimmerLoadingState extends State<_ShimmerLoading>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1450),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
    animation: _controller,
    child: widget.child,
    builder: (context, child) => ShaderMask(
      blendMode: BlendMode.srcATop,
      shaderCallback: (bounds) {
        final shift = -1.8 + _controller.value * 3.6;
        return LinearGradient(
          begin: Alignment(shift, -.24),
          end: Alignment(shift + .82, .24),
          colors: const [
            Color(0xFFEADBCB),
            Color(0xFFF8F1E8),
            Color(0xFFD7C0BE),
            Color(0xFFEADBCB),
          ],
          stops: const [0, .40, .58, 1],
        ).createShader(bounds);
      },
      child: child,
    ),
  );
}
