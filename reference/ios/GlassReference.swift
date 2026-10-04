import CoreImage
import ReplayKit
import SwiftUI
import UIKit

/// The system's Liquid Glass over the same content as the Android sample, the reference the library is measured against.
@main
struct GlassReferenceApp: App {
    init() {
        KeepAwake.start()
    }

    var body: some Scene {
        WindowGroup {
            if ReferenceLaunch.probe {
                MaterialProbe()
            } else if ReferenceLaunch.controls {
                ControlsProbe()
            } else if ReferenceLaunch.menus {
                MenusProbe()
            } else if let exhibit = ReferenceLaunch.exhibit {
                GalleryScreen(showing: exhibit)
            } else if ReferenceLaunch.measuring {
                ReferenceScreen()
            } else {
                GalleryScreen()
            }
        }
    }
}

/// Keeps the device from locking while the app is in front, and restarts the idle countdown whenever the Mac posts
/// the `tv.quven.glass.keepawake` Darwin notification (`keep-awake.sh` posts it every 60 seconds).
enum KeepAwake {
    static let notification = "tv.quven.glass.keepawake"

    static func start() {
        UIApplication.shared.isIdleTimerDisabled = true
        CFNotificationCenterAddObserver(
            CFNotificationCenterGetDarwinNotifyCenter(),
            nil,
            { _, _, _, _, _ in
                DispatchQueue.main.async {
                    UIApplication.shared.isIdleTimerDisabled = false
                    UIApplication.shared.isIdleTimerDisabled = true
                }
            },
            notification as CFString,
            nil,
            .deliverImmediately
        )
    }
}

/// The launch settings, read from the environment (`SIMCTL_CHILD_GLASS_*` in the simulator) or the arguments.
enum ReferenceLaunch {
    /// The distance the content starts scrolled by, in points.
    static let scroll = CGFloat(value("GLASS_SCROLL") ?? 0)
    /// The index of the bar's held entry.
    static let tab = Int(value("GLASS_TAB") ?? 0)
    /// The space between the bar's capsule and its Search circle, in points.
    static let gap = CGFloat(value("GLASS_GAP") ?? 8)
    /// The name of the capture to save into Documents once the screen settles, or `nil` for none.
    static let capture = ProcessInfo.processInfo.environment["GLASS_CAPTURE"]
    /// The seconds to record every frame for, while someone presses the bar, or `nil` for none.
    static let record = value("GLASS_RECORD")
    /// The part of the screen a recording keeps, in points from the top-left corner (`GLASS_REGION=x,y,w,h`); the bar
    /// and its surroundings unless named.
    static let region: CGRect = {
        let parts = (ProcessInfo.processInfo.environment["GLASS_REGION"] ?? "").split(separator: ",").compactMap { Double($0) }
        return parts.count == 4 ? CGRect(x: parts[0], y: parts[1], width: parts[2], height: parts[3]) : CGRect(x: 260, y: 680, width: 660, height: 130)
    }()
    /// Whether the app shows the material probe, glass of several sizes over flat colours, instead of the screen.
    static let probe = ProcessInfo.processInfo.environment["GLASS_PROBE"] != nil
    /// The circle sizes the probe draws, in points (`GLASS_PROBE_SIZES=36,51,...`).
    static let probeSizes: [CGFloat] = (ProcessInfo.processInfo.environment["GLASS_PROBE_SIZES"] ?? "36,51,70,100")
        .split(separator: ",")
        .compactMap { Double($0).map { CGFloat($0) } }
    /// The glass the probe draws: `regular`, `clear`, or a tint as `tint:RRGGBB` or `clear-tint:RRGGBB`
    /// (`GLASS_PROBE_GLASS`); regular unless named.
    static let probeGlass: Glass = {
        let name = ProcessInfo.processInfo.environment["GLASS_PROBE_GLASS"] ?? "regular"
        let parts = name.split(separator: ":")
        let base: Glass = parts.first.map { $0.hasPrefix("clear") } == true ? .clear : .regular
        guard parts.count == 2, let rgb = UInt32(parts[1], radix: 16) else { return base }
        return base.tint(Color(argb: 0xFF00_0000 | rgb))
    }()
    /// Whether each probe circle carries a glyph in the primary style beside one in explicit white.
    static let probeInk = ProcessInfo.processInfo.environment["GLASS_PROBE_INK"] != nil
    /// The seconds to keep every frame of the region for, a second after launch, whatever is pressed, or `nil` for none.
    static let window = value("GLASS_WINDOW")
    /// The number of presses a recording keeps a clip of before it ends.
    static let taps = Int(value("GLASS_TAPS") ?? 4)
    /// Whether the app shows the system's menus over the screen's content: a pull-down from a glass disc and a card's
    /// context menu, with every kind of entry a menu can hold.
    static let menus = ProcessInfo.processInfo.environment["GLASS_MENUS"] != nil
    /// Whether the app shows the system's own glass controls, a tab bar and a segmented control, instead of the screen.
    static let controls = ProcessInfo.processInfo.environment["GLASS_CONTROLS"] != nil
    /// Whether the system's controls stand over a white page instead of the screen's content.
    static let controlsOverWhite = ProcessInfo.processInfo.environment["GLASS_CONTROLS_WHITE"] != nil
    /// Whether the app was launched to be measured, with a `GLASS_` setting in its environment or arguments; launched
    /// from the Home Screen it shows the gallery.
    static let measuring = ProcessInfo.processInfo.environment.keys.contains { $0.hasPrefix("GLASS_") }
        || ProcessInfo.processInfo.arguments.contains { $0.hasPrefix("-GLASS_") }
    /// The gallery exhibit to open on, named as its case (`GLASS_EXHIBIT=clearAndTinted`), captured as `GLASS_CAPTURE`
    /// and `GLASS_WINDOW` say, or `nil` for none.
    static let exhibit = ProcessInfo.processInfo.environment["GLASS_EXHIBIT"].flatMap { name in
        Exhibit.allCases.first { String(describing: $0) == name }
    }
    /// The scroll offsets, in points, to capture one after the other.
    static let scrolls: [CGFloat] = (ProcessInfo.processInfo.environment["GLASS_SCROLLS"] ?? "")
        .split(separator: ",")
        .compactMap { Double($0).map { CGFloat($0) } }

    private static func value(_ name: String) -> Double? {
        if let text = ProcessInfo.processInfo.environment[name] { return Double(text) }
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: "-\(name)"), index + 1 < arguments.count else { return nil }
        return Double(arguments[index + 1])
    }
}

extension Color {
    /// Creates a colour from a packed `0xAARRGGBB` value, as Compose writes it.
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}

enum Palette {
    static let ground = Color(argb: 0xFF0A_0A0A)
    static let textHigh = Color(argb: 0xFFF5_F5F5)
    static let textMedium = Color(argb: 0xFFA8_A8A8)
    static let accent = Color(argb: 0xFFE8_540E)
}

struct Poster {
    let title: String
    let top: Color
    let bottom: Color
    let mark: Color
    let text: Color
}

let posters: [Poster] = [
    Poster(title: "Solstice", top: Color(argb: 0xFFFF_F3B0), bottom: Color(argb: 0xFFFF_C300), mark: Color(argb: 0xFFFF_FFFF), text: Color(argb: 0xFF1A_1A1A)),
    Poster(title: "Abyss", top: Color(argb: 0xFF0B_1A3A), bottom: Color(argb: 0xFF00_0000), mark: Color(argb: 0xFF2E_6BFF), text: .white),
    Poster(title: "Crimson", top: Color(argb: 0xFFFF_2D55), bottom: Color(argb: 0xFF6A_0018), mark: Color(argb: 0xFFFF_B3C1), text: .white),
    Poster(title: "Snowfield", top: Color(argb: 0xFFFF_FFFF), bottom: Color(argb: 0xFFE6_F0FF), mark: Color(argb: 0xFF9E_C5FF), text: Color(argb: 0xFF10_1010)),
    Poster(title: "Verdant", top: Color(argb: 0xFF00_C853), bottom: Color(argb: 0xFF00_3D1A), mark: Color(argb: 0xFFB9_FFD1), text: .white),
    Poster(title: "Nightfall", top: Color(argb: 0xFF11_1111), bottom: Color(argb: 0xFF00_0000), mark: Color(argb: 0xFFE8_540E), text: .white),
    Poster(title: "Lagoon", top: Color(argb: 0xFF00_E5FF), bottom: Color(argb: 0xFF00_40FF), mark: Color(argb: 0xFFFF_FFFF), text: .white),
    Poster(title: "Ember", top: Color(argb: 0xFFFF_AB00), bottom: Color(argb: 0xFFD5_0000), mark: Color(argb: 0xFFFF_F59D), text: .white),
    Poster(title: "Lilac", top: Color(argb: 0xFFE1_BEE7), bottom: Color(argb: 0xFF7B_1FA2), mark: Color(argb: 0xFFFF_FFFF), text: .white),
]

struct ReferenceScreen: View {
    @State private var position = ScrollPosition(y: ReferenceLaunch.scroll)
    @State private var tab = ReferenceLaunch.tab
    @State private var density = 1
    @State private var recording = false
    @State private var presses = AsyncStream.makeStream(of: Void.self)

    var body: some View {
        ZStack {
            ScrollView {
                BackdropContent()
            }
            .scrollPosition($position)
            .background(Palette.ground)
            .ignoresSafeArea()

            VStack {
                HStack(spacing: 12) {
                    Menu {
                        LibraryMenuEntries()
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(Palette.textHigh)
                            .frame(width: 56, height: 56)
                    }
                    .buttonStyle(.glass)
                    .buttonBorderShape(.circle)
                    .accessibilityIdentifier("reference.gear")
                    DensityTrack(held: $density)
                    Spacer()
                    if recording {
                        Text("REC").font(.system(size: 15, weight: .bold)).foregroundStyle(.white)
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(Capsule().fill(.red))
                    }
                }
                .padding(16)
                Spacer()
                ReferenceBar(held: $tab)
                    .padding(.bottom, 8)
            }
        }
        .preferredColorScheme(.dark)
        .onChange(of: tab) { presses.continuation.yield() }
        .task {
            guard let name = ReferenceLaunch.capture else { return }
            let screen = ScreenCapture()
            guard await screen.start() else { return }
            if let seconds = ReferenceLaunch.window {
                position = ScrollPosition(y: ReferenceLaunch.scroll)
                await screen.keepWindow(named: name, seconds: seconds)
                screen.stop()
                return
            }
            if let seconds = ReferenceLaunch.record {
                position = ScrollPosition(y: ReferenceLaunch.scroll)
                try? await Task.sleep(for: .seconds(1))
                recording = true
                await screen.recordPresses(presses.stream, seconds: seconds, named: name)
                recording = false
                screen.stop()
                return
            }
            for offset in ReferenceLaunch.scrolls.isEmpty ? [ReferenceLaunch.scroll] : ReferenceLaunch.scrolls {
                position = ScrollPosition(y: offset)
                try? await Task.sleep(for: .seconds(1.5))
                screen.save(named: "\(name)-\(Int(offset))")
            }
            screen.stop()
        }
    }
}

/// Records the screen through ReplayKit, the frames the display shows with the glass composited, and saves the latest
/// frame into the app's Documents folder on demand; while recording, it keeps every frame of a region instead.
final class ScreenCapture: @unchecked Sendable {
    /// A frame kept while recording, with the moment the display showed it.
    struct Frame {
        let time: CMTime
        let image: CGImage
    }

    private let lock = NSLock()
    private var latest: CGImage?
    private var reel: [Frame]?
    private var region = CGRect.null
    private var pointsWide: CGFloat = 1
    private var keeping = CMTime.positiveInfinity
    private let context = CIContext()
    private static var documents: URL { FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0] }
    private var documents: URL { Self.documents }

    /// Starts recording; returns whether the recording started.
    func start() async -> Bool {
        await withCheckedContinuation { (done: CheckedContinuation<Bool, Never>) in
            RPScreenRecorder.shared().startCapture(handler: { [self] buffer, type, _ in
                guard type == .video, let pixels = CMSampleBufferGetImageBuffer(buffer) else { return }
                let image = CIImage(cvPixelBuffer: pixels)
                if let (points, wide) = lock.withLock({ reel == nil ? nil : (region, pointsWide) }) {
                    // The region is in points from the top-left corner; the image's origin is its bottom-left corner.
                    let scale = image.extent.width / wide
                    let crop = CGRect(x: points.minX * scale, y: image.extent.height - points.maxY * scale, width: points.width * scale, height: points.height * scale)
                    guard let frame = context.createCGImage(image, from: crop.integral) else { return }
                    let time = CMSampleBufferGetPresentationTimeStamp(buffer)
                    lock.withLock {
                        reel?.append(Frame(time: time, image: frame))
                        reel?.removeAll { CMTimeCompare(CMTimeSubtract(time, $0.time), keeping) > 0 }
                    }
                    return
                }
                guard let frame = context.createCGImage(image, from: image.extent) else { return }
                lock.withLock { latest = frame }
            }, completionHandler: { [self] error in
                if let error {
                    try? "\(error)".write(to: documents.appendingPathComponent("capture-error.txt"), atomically: true, encoding: .utf8)
                }
                done.resume(returning: error == nil)
            })
        }
    }

    /// Saves the latest frame as `<name>.png`.
    func save(named name: String) {
        guard let frame = lock.withLock({ latest }) else { return }
        try? UIImage(cgImage: frame).pngData()?.write(to: documents.appendingPathComponent("\(name).png"))
    }

    /// Starts keeping every frame of `region`, in points from the screen's top-left corner, until `endRecording()`;
    /// frames older than `seconds` before the newest are let go.
    @MainActor
    func beginRecording(region: CGRect, keeping seconds: Double = .infinity) {
        let wide = UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.screen.bounds.width }.first ?? 1180
        lock.withLock {
            self.region = region
            pointsWide = wide
            keeping = seconds.isFinite ? CMTime(seconds: seconds, preferredTimescale: 600) : .positiveInfinity
            reel = []
        }
    }

    /// Returns the frames kept so far and goes on keeping them.
    func recorded() -> [Frame] {
        lock.withLock { reel ?? [] }
    }

    /// Keeps every frame of `ReferenceLaunch.region` for `seconds`, from a second after it is called, written as
    /// `<name>-window-000.png` onwards.
    @MainActor
    func keepWindow(named name: String, seconds: Double) async {
        try? await Task.sleep(for: .seconds(1))
        beginRecording(region: ReferenceLaunch.region)
        try? await Task.sleep(for: .seconds(seconds))
        await Self.write(endRecording(), named: "\(name)-window")
    }

    /// Keeps, for each of the first `ReferenceLaunch.taps` presses `presses` tells, a clip of the launch region from a
    /// second before it to `seconds` after it, written as `<name>-<press>-000.png` onwards.
    @MainActor
    func recordPresses(_ presses: AsyncStream<Void>, seconds: Double, named name: String) async {
        beginRecording(region: ReferenceLaunch.region, keeping: 1 + seconds)
        var index = 0
        for await _ in presses {
            try? await Task.sleep(for: .seconds(seconds))
            await Self.write(recorded(), named: "\(name)-\(index)")
            index += 1
            if index == ReferenceLaunch.taps { break }
        }
        _ = endRecording()
    }

    /// Stops keeping frames and returns the frames kept.
    func endRecording() -> [Frame] {
        lock.withLock {
            defer { reel = nil }
            return reel ?? []
        }
    }

    /// Writes `reel` as `<name>-000.png` onwards, with `<name>-times.txt` holding each frame's milliseconds after the
    /// first, away from the main thread.
    static func write(_ reel: [Frame], named name: String) async {
        await Task.detached(priority: .utility) {
            guard let first = reel.first?.time else { return }
            var times = ""
            for (index, frame) in reel.enumerated() {
                try? UIImage(cgImage: frame.image).pngData()?.write(to: documents.appendingPathComponent(String(format: "\(name)-%03d.png", index)))
                times += String(format: "%d %.2f\n", index, CMTimeGetSeconds(CMTimeSubtract(frame.time, first)) * 1000)
            }
            try? times.write(to: documents.appendingPathComponent("\(name)-times.txt"), atomically: true, encoding: .utf8)
        }.value
    }

    /// Stops recording.
    func stop() {
        RPScreenRecorder.shared().stopCapture { _ in }
    }

}

struct Header: View {
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Stops every sixteenth of the way, blended in sRGB, so the gradient matches the Android sample's whatever
            // space the system blends in.
            LinearGradient(
                colors: [
                    Color(argb: 0xFF3A_0CA3),
                    Color(argb: 0xFF52_0F9F),
                    Color(argb: 0xFF69_129C),
                    Color(argb: 0xFF81_1598),
                    Color(argb: 0xFF98_1894),
                    Color(argb: 0xFFB0_1C90),
                    Color(argb: 0xFFC8_1F8C),
                    Color(argb: 0xFFDF_2289),
                    Color(argb: 0xFFF7_2585),
                    Color(argb: 0xFFF8_3B76),
                    Color(argb: 0xFFF9_5166),
                    Color(argb: 0xFFFA_6757),
                    Color(argb: 0xFFFB_7E48),
                    Color(argb: 0xFFFC_9438),
                    Color(argb: 0xFFFD_AA29),
                    Color(argb: 0xFFFE_C019),
                    Color(argb: 0xFFFF_D60A)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            VStack(alignment: .leading, spacing: 0) {
                Text("Quven Glass").font(.system(size: 56, weight: .black)).foregroundStyle(.white)
                Text("Liquid Glass for Compose").font(.system(size: 22)).foregroundStyle(.white.opacity(0.85))
            }
            .padding(24)
        }
        .frame(height: 280)
    }
}

struct PosterRow: View {
    let offset: Int

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(0..<posters.count, id: \.self) { index in
                    PosterCard(poster: posters[(index + offset) % posters.count])
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }
}

struct PosterCard: View {
    let poster: Poster

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(colors: [poster.top, poster.bottom], startPoint: .top, endPoint: .bottom)
            Canvas { context, size in
                let big = size.width * 0.32
                context.fill(
                    Path(ellipseIn: CGRect(x: size.width * 0.62 - big, y: size.height * 0.36 - big, width: big * 2, height: big * 2)),
                    with: .color(poster.mark)
                )
                let small = size.width * 0.18
                context.fill(
                    Path(ellipseIn: CGRect(x: size.width * 0.25 - small, y: size.height * 0.62 - small, width: small * 2, height: small * 2)),
                    with: .color(poster.mark.opacity(0.4))
                )
            }
            Text(poster.title.uppercased())
                .font(.system(size: 22, weight: .heavy))
                .foregroundStyle(poster.text)
                .padding(12)
        }
        .frame(width: 170, height: 255)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .circular))
    }
}

struct TextBlock: View {
    var body: some View {
        Text(
            "The glass bends what lies under it towards its rim, blurs it a little, parts its colours along the edge and "
                + "lifts its saturation. Over bright posters it darkens so the labels stay legible; over dark ones it stays "
                + "clear. Scroll this text under the bar to see it bend."
        )
        .font(.system(size: 20))
        .lineSpacing(6)
        .foregroundStyle(Palette.textHigh)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .frame(height: 150, alignment: .top)
        .clipped()
    }
}

struct Bands: View {
    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .leading) {
                Color.white
                Text("A WHITE BAND").font(.system(size: 30, weight: .bold)).foregroundStyle(.black).padding(.leading, 24)
            }
            .frame(height: 90)
            Canvas { context, size in
                var x: CGFloat = 0
                var dark = false
                while x < size.width {
                    context.fill(Path(CGRect(x: x, y: 0, width: 6, height: size.height)), with: .color(dark ? .black : .white))
                    x += 6
                    dark.toggle()
                }
            }
            .frame(height: 120)
            HStack(spacing: 0) {
                ForEach([0xFFFF_1744, 0xFF00_E676, 0xFF29_79FF, 0xFFFF_EA00, 0xFF00_0000] as [UInt32], id: \.self) { argb in
                    Color(argb: argb)
                }
            }
            .frame(height: 90)
        }
    }
}

/// White lines every 12 points and red ones every 48 on black, 240 points tall, to measure how the glass moves what
/// lies under it.
struct CalibrationGrid: View {
    var body: some View {
        Canvas { context, size in
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black))
            var x: CGFloat = 0
            var index = 0
            while x < size.width {
                let red = index % 4 == 0
                context.fill(Path(CGRect(x: x, y: 0, width: 2, height: size.height)), with: .color(red ? .red : .white))
                x += 12
                index += 1
            }
            var y: CGFloat = 0
            index = 0
            while y < size.height {
                let red = index % 4 == 0
                context.fill(Path(CGRect(x: 0, y: y, width: size.width, height: 2)), with: .color(red ? .red : .white))
                y += 12
                index += 1
            }
        }
        .frame(height: 240)
    }
}

/// A grey sawtooth from black to white every `period` points, across or down, whose shade tells where a pixel seen
/// through the glass was taken from.
struct CalibrationRamp: View {
    let horizontal: Bool
    let period: CGFloat

    var body: some View {
        Canvas { context, size in
            let extent = horizontal ? size.width : size.height
            var start: CGFloat = 0
            while start < extent {
                let gradient = Gradient(colors: [.black, .white])
                let rect = horizontal
                    ? CGRect(x: start, y: 0, width: period, height: size.height)
                    : CGRect(x: 0, y: start, width: size.width, height: period)
                let from = horizontal ? CGPoint(x: rect.minX, y: 0) : CGPoint(x: 0, y: rect.minY)
                let to = horizontal ? CGPoint(x: rect.maxX, y: 0) : CGPoint(x: 0, y: rect.maxY)
                context.fill(Path(rect), with: .linearGradient(gradient, startPoint: from, endPoint: to))
                start += period
            }
        }
    }
}

/// One grey ramp, black to white over 120 points: across, centred on the left rim of the bar's capsule (287 points left
/// of the window's centre); down, from 40 to 160 points below the band's top. Its shade tells, without ambiguity, where
/// a pixel seen through the glass was taken from.
struct CalibrationProbe: View {
    let horizontal: Bool

    var body: some View {
        Canvas { context, size in
            let gradient = Gradient(colors: [.black, .white])
            let rim = size.width / 2 - 287
            let from = horizontal ? CGPoint(x: rim - 60, y: 0) : CGPoint(x: 0, y: 40)
            let to = horizontal ? CGPoint(x: rim + 60, y: 0) : CGPoint(x: 0, y: 160)
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(gradient, startPoint: from, endPoint: to))
        }
    }
}

struct BarEntry: Identifiable {
    let label: String
    let symbol: String
    var id: String { label }
}

let entries = [
    BarEntry(label: "Home", symbol: "house.fill"),
    BarEntry(label: "Watch", symbol: "play.fill"),
    BarEntry(label: "Favorites", symbol: "star.fill"),
    BarEntry(label: "Explore", symbol: "info.circle.fill"),
    BarEntry(label: "More", symbol: "ellipsis"),
]

/// A tablet bar: five entries in a glass capsule with a sliding platter, Search in a glass circle beside it.
struct ReferenceBar: View {
    @Binding var held: Int
    @Namespace private var platter

    var body: some View {
        GlassEffectContainer(spacing: ReferenceLaunch.gap) {
            HStack(spacing: ReferenceLaunch.gap) {
                HStack(spacing: 2) {
                    ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
                        Button {
                            withAnimation(.spring(response: 0.34, dampingFraction: 0.82)) { held = index }
                        } label: {
                            face(entry, selected: index == held, showsLabel: true)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(4)
                .glassEffect(.regular.interactive(), in: Capsule())

                face(BarEntry(label: "Search", symbol: "magnifyingglass"), selected: false, showsLabel: false)
                    .padding(4)
                    .glassEffect(.regular.interactive(), in: Circle())
            }
        }
    }

    private func face(_ entry: BarEntry, selected: Bool, showsLabel: Bool) -> some View {
        VStack(spacing: 1) {
            Image(systemName: entry.symbol)
                .font(.system(size: 24, weight: .semibold))
                .frame(height: 29)
            if showsLabel {
                Text(entry.label)
                    .font(.system(size: 13, weight: selected ? .semibold : .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
        }
        .foregroundStyle(selected ? Palette.accent : Palette.textHigh)
        .frame(width: showsLabel ? 96 : 62, height: 62)
        .background {
            if selected {
                Capsule().fill(Palette.textHigh.opacity(0.12)).matchedGeometryEffect(id: "selection", in: platter)
            }
        }
        .contentShape(Capsule())
    }
}

/// A catalogue's density selector on a Liquid Glass track.
struct DensityTrack: View {
    @Binding var held: Int
    @Namespace private var pill

    var body: some View {
        HStack(spacing: 3) {
            ForEach([3, 2, 1], id: \.self) { columns in
                let index = [3, 2, 1].firstIndex(of: columns)!
                Button {
                    withAnimation(.spring(response: 0.28, dampingFraction: 0.62)) { held = index }
                } label: {
                    Canvas { context, size in
                        let cell = size.width / CGFloat(columns)
                        for row in 0..<columns {
                            for column in 0..<columns {
                                let rect = CGRect(x: CGFloat(column) * cell + cell * 0.1, y: CGFloat(row) * cell + cell * 0.1, width: cell * 0.8, height: cell * 0.8)
                                context.fill(Path(roundedRect: rect, cornerRadius: cell * 0.18), with: .color(index == held ? Palette.textHigh : Palette.textMedium))
                            }
                        }
                    }
                    .frame(width: 20, height: 20)
                    .frame(width: 52, height: 44)
                    .background {
                        if index == held {
                            Capsule()
                                .fill(Palette.textHigh.opacity(0.2))
                                .overlay(Capsule().stroke(Palette.textHigh.opacity(0.35), lineWidth: 1))
                                .shadow(color: .black.opacity(0.35), radius: 4, y: 1)
                                .matchedGeometryEffect(id: "pill", in: pill)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .glassEffect(.regular, in: Capsule())
    }
}

/// Glass circles of four sizes, still and interactive, over columns of flat colour: one capture shows how the material
/// depends on an element's size and interactivity, read where each circle's middle stands over a single colour.
struct MaterialProbe: View {
    static let colours: [Color] = [.white, Color(white: 0.5), .black, Color(argb: 0xFFFF_1744), Color(argb: 0xFF00_E676), Color(argb: 0xFF29_79FF), Color(argb: 0xFFFF_EA00)]
    static let sizes = ReferenceLaunch.probeSizes
    // Interactive glass reads as still glass does, so a long list of sizes keeps one column of them.
    static let variants = sizes.count > 4 ? [false] : [false, true]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(Self.colours.enumerated()), id: \.offset) { _, colour in
                VStack(spacing: 12) {
                    ForEach(Self.variants, id: \.self) { interactive in
                        ForEach(Self.sizes, id: \.self) { size in
                            ProbeInk(shown: ReferenceLaunch.probeInk)
                                .font(.system(size: size * 0.24, weight: .semibold))
                                .frame(width: size, height: size)
                                .glassEffect(interactive ? ReferenceLaunch.probeGlass.interactive() : ReferenceLaunch.probeGlass, in: Circle())
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(colour)
            }
        }
        .ignoresSafeArea()
        .preferredColorScheme(.dark)
        .task {
            guard let name = ReferenceLaunch.capture else { return }
            let screen = ScreenCapture()
            guard await screen.start() else { return }
            try? await Task.sleep(for: .seconds(1.5))
            screen.save(named: name)
            screen.stop()
        }
    }
}

/// Red where the content's colour scheme is light, blue where it is dark.
struct SchemeProbe: ShapeStyle {
    func resolve(in environment: EnvironmentValues) -> Color {
        environment.colorScheme == .light ? .red : .blue
    }
}

/// Glyphs in the primary style, in explicit white, telling the colour scheme, in the secondary and in the tertiary style, or nothing.
struct ProbeInk: View {
    let shown: Bool

    var body: some View {
        if shown {
            HStack(spacing: 2) {
                Image(systemName: "square.grid.2x2.fill").foregroundStyle(.primary)
                Image(systemName: "square.grid.2x2.fill").foregroundStyle(.white)
                Image(systemName: "square.grid.2x2.fill").foregroundStyle(SchemeProbe())
                Image(systemName: "square.fill").foregroundStyle(.secondary)
                Image(systemName: "square.fill").foregroundStyle(.tertiary)
            }
        } else {
            Color.clear
        }
    }
}

/// The page under the system's controls: the screen's content, or a white page.
private struct ControlsPage: View {
    @Binding var segment: Int

    var body: some View {
        if ReferenceLaunch.controlsOverWhite {
            Color.white.ignoresSafeArea()
        } else {
            ScrollView {
                VStack(spacing: 0) {
                    Picker("Density", selection: $segment) {
                        ForEach(0..<3, id: \.self) { option in
                            Image(systemName: ["square.grid.3x3.fill", "square.grid.2x2.fill", "square.fill"][option]).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 300)
                    .padding(.vertical, 24)
                    PosterRow(offset: 0)
                    Bands()
                    PosterRow(offset: 3)
                }
            }
            .background(Palette.ground)
        }
    }
}

/// The system's own Liquid Glass controls over the same content: a tab bar and a segmented control, whose moves between
/// options a recording keeps a clip of, press by press.
struct ControlsProbe: View {
    @State private var tab = 0
    @State private var segment = 1
    @State private var recording = false
    @State private var presses = AsyncStream.makeStream(of: Void.self)

    var body: some View {
        TabView(selection: $tab) {
            ForEach(Array(entries.enumerated()), id: \.offset) { index, entry in
                Tab(entry.label, systemImage: entry.symbol, value: index) {
                    ControlsPage(segment: $segment)
                }
            }
        }
        .overlay(alignment: .topTrailing) {
            if recording {
                Text("REC").font(.system(size: 15, weight: .bold)).foregroundStyle(.white)
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Capsule().fill(.red))
                    .padding(16)
            }
        }
        .preferredColorScheme(.dark)
        .onChange(of: tab) { presses.continuation.yield() }
        .onChange(of: segment) { presses.continuation.yield() }
        .task {
            guard let name = ReferenceLaunch.capture, let seconds = ReferenceLaunch.record else { return }
            let screen = ScreenCapture()
            guard await screen.start() else { return }
            try? await Task.sleep(for: .seconds(1))
            recording = true
            await screen.recordPresses(presses.stream, seconds: seconds, named: name)
            recording = false
            screen.stop()
        }
    }
}

/// The system's menus over the screen's content: a pull-down from a glass disc holding a choice, a toggle, a disabled
/// entry, an entry with a second line, a submenu and a destructive entry, and a card's context menu.
struct MenusProbe: View {
    @State private var sort = 1
    @State private var ascending = true

    var body: some View {
        ZStack(alignment: .topLeading) {
            ScrollView {
                VStack(spacing: 0) {
                    Header()
                    PosterRow(offset: 0)
                    TextBlock()
                    PosterRow(offset: 3)
                }
            }
            .background(Palette.ground)
            .ignoresSafeArea()

            HStack(alignment: .top) {
                PosterCard(poster: posters[2])
                    .contextMenu {
                        Button("Play", systemImage: "play.fill") {}
                        Button("Details", systemImage: "info.circle") {}
                        Button("Save", systemImage: "bookmark") {}
                    }
                    .accessibilityIdentifier("probe.card")
                Menu {
                    Button("15 minutes") {}
                    Button("30 minutes") {}
                    Button("End of chapter") {}
                    Button("Off", role: .destructive) {}
                } label: {
                    Image(systemName: "moon.zzz")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 46, height: 46)
                        .glassEffect(.regular.interactive(), in: Circle())
                }
                .accessibilityIdentifier("probe.plain")
                .padding(.leading, 40)
                Spacer()
                Menu {
                    Section("Sort by") {
                        Picker("Sort by", selection: $sort) {
                            Text("Title").tag(0)
                            Text("Year").tag(1)
                            Text("Added").tag(2)
                        }
                        .pickerStyle(.inline)
                        Toggle("Ascending", isOn: $ascending)
                    }
                    Section {
                        Button {} label: {
                            Label {
                                Text("Download")
                                Text("2.4 GB")
                            } icon: {
                                Image(systemName: "arrow.down.circle")
                            }
                        }
                        Button("Unavailable", systemImage: "nosign") {}
                            .disabled(true)
                        Menu("More", systemImage: "ellipsis.circle") {
                            Button("First", systemImage: "1.circle") {}
                            Button("Second", systemImage: "2.circle") {}
                        }
                        Button("Remove", systemImage: "trash", role: .destructive) {}
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 46, height: 46)
                        .glassEffect(.regular.interactive(), in: Circle())
                }
                .accessibilityIdentifier("probe.overflow")
            }
            .padding(.horizontal, 24)
            .padding(.top, 360)
        }
        .preferredColorScheme(.dark)
    }
}

/// The entries of the gear's menu: two titled sections of glyphed rows, the last one destructive.
struct LibraryMenuEntries: View {
    var body: some View {
        Section("Library") {
            Button("Collections", systemImage: "play.rectangle.on.rectangle.fill") {}
            Button("Saved", systemImage: "bookmark.fill") {}
            Button("Bookshelf", systemImage: "books.vertical.fill") {}
        }
        Section("Account") {
            Button("Profile", systemImage: "person.fill") {}
            Button("Sync", systemImage: "arrow.triangle.2.circlepath") {}
            Button("Switch view", systemImage: "rectangle.2.swap") {}
            Button("Settings", systemImage: "gearshape.fill") {}
            Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {}
        }
    }
}

// MARK: - Gallery

/// How far along the Android library is with one of the system's Liquid Glass elements.
enum ExhibitStatus: CaseIterable {
    /// The library draws the element.
    case ready
    /// The element is being built.
    case inDevelopment
    /// The element waits on a decision before it is built.
    case planned

    /// The name the gallery shows for the status.
    var label: String {
        switch self {
        case .ready: "Ready"
        case .inDevelopment: "In development"
        case .planned: "Planned"
        }
    }

    /// The colour the gallery marks the status with.
    var color: Color {
        switch self {
        case .ready: Color(argb: 0xFF34_C759)
        case .inDevelopment: Color(argb: 0xFFFF_9F0A)
        case .planned: Color(argb: 0xFF8E_8E93)
        }
    }
}

/// One of the system's Liquid Glass elements, in the order and under the names the Android sample's gallery lists them.
enum Exhibit: CaseIterable, Identifiable {
    case material, glassButtons, tabBar, segmentedControl, joiningGlass, menus, morphingPanel, clearAndTinted, capsuleButtons
    case toggle, slider, contextMenu, submenus, toolbar, sheet, alert, popover, search
    case minimizingTabBar, bottomAccessory, scrollEdge, touchLight, textMenu
    case adaptiveSidebar

    var id: Self { self }

    /// The element's name.
    var title: String {
        switch self {
        case .material: "Material"
        case .glassButtons: "Glass buttons"
        case .tabBar: "Tab bar"
        case .segmentedControl: "Segmented control"
        case .joiningGlass: "Joining glass"
        case .menus: "Menus"
        case .morphingPanel: "Morphing panel"
        case .contextMenu: "Context menu"
        case .submenus: "Submenus"
        case .capsuleButtons: "Capsule buttons"
        case .toolbar: "Toolbar"
        case .toggle: "Switch"
        case .slider: "Slider"
        case .sheet: "Sheet"
        case .alert: "Alert"
        case .popover: "Popover"
        case .search: "Search"
        case .minimizingTabBar: "Minimizing tab bar"
        case .bottomAccessory: "Bottom accessory"
        case .scrollEdge: "Scroll edge"
        case .clearAndTinted: "Clear and tinted glass"
        case .touchLight: "Touch light"
        case .textMenu: "Text menu"
        case .adaptiveSidebar: "Adaptive sidebar"
        }
    }

    /// What the element is and does.
    var summary: String {
        switch self {
        case .material:
            "Glass bends the content at its rim, blurs and tones it, and catches the light on its edge. Up to 63 dp it is "
                + "thin glass, which turns light over a bright page; from 66 dp it is thick glass."
        case .glassButtons: "Round controls of glass that swell and light the content under them while pressed."
        case .tabBar:
            "Entries in a capsule beside a Search circle. The held pill slides with a stretch, a press lifts it into a lens "
                + "and a drag carries it to the entry let go over."
        case .segmentedControl: "Options on a glass track whose pill slides, stretches and lifts as the tab bar's does."
        case .joiningGlass:
            "Surfaces closer than their container's spacing flow into one piece of glass, and part again as they move apart."
        case .menus:
            "Menus grow out of their control: titles, glyphs, choices, disabled and destructive entries, and plain menus. "
                + "A menu with more room above rises, its entries reversed. Slide a finger along the rows to choose."
        case .morphingPanel:
            "A control opens into a panel of any content as one piece of glass and closes back into it. This panel tunes "
                + "the material of every exhibit."
        case .contextMenu: "A long press lifts the card out of the page, dims the rest and opens the card's menu beside it."
        case .submenus: "An entry that opens a second menu at the first one's side, and entries that carry a second line."
        case .capsuleButtons: "Buttons of clear glass and of prominent, tinted glass, holding a name, a glyph or both."
        case .toolbar: "Glass buttons along the top of a page, grouped into capsules that join and part."
        case .toggle: "A switch whose thumb lifts into a lens of glass while it is held or dragged."
        case .slider: "A slider whose thumb lifts into a lens of glass while it is dragged."
        case .sheet: "A sheet of glass that rises from the bottom edge and turns opaque as it is drawn to full height."
        case .alert: "A dialog on glass over a dimmed page, its buttons capsules."
        case .popover: "A panel of glass that grows out of the control it belongs to and points at it."
        case .search: "The Search circle opens into a field of glass along the bar."
        case .minimizingTabBar:
            "On a phone the tab bar shrinks to its held entry while the content scrolls down, and grows back as it scrolls up."
        case .bottomAccessory: "A strip of glass above the tab bar, such as a player's controls, that shrinks with the bar."
        case .scrollEdge: "Content fades and blurs as it passes under the bars at the top and bottom of a page."
        case .clearAndTinted: "The clear variant, which lets bright media through, and glass tinted with a colour."
        case .touchLight: "Light that gathers under the finger and follows it across interactive glass."
        case .textMenu: "The menu of cut, copy and paste on glass, over selected text."
        case .adaptiveSidebar: "A sidebar of glass floating over the content, which becomes a tab bar in a narrow window."
        }
    }

    /// How far along the Android library is with the element.
    var status: ExhibitStatus {
        switch self {
        case .material, .glassButtons, .tabBar, .segmentedControl, .joiningGlass, .menus, .morphingPanel, .clearAndTinted,
             .capsuleButtons, .toggle, .slider, .contextMenu: .ready
        case .adaptiveSidebar: .planned
        default: .inDevelopment
        }
    }
}

/// Lists the system's Liquid Glass elements beside the one chosen, each drawn by the system over hard content.
struct GalleryScreen: View {
    @State private var chosen: Exhibit

    /// Creates the gallery.
    /// - Parameter exhibit: The exhibit shown first.
    init(showing exhibit: Exhibit = .material) {
        _chosen = State(initialValue: exhibit)
    }

    var body: some View {
        HStack(spacing: 0) {
            ExhibitList(chosen: $chosen)
                .frame(width: 300)
            ExhibitPage(exhibit: chosen)
        }
        .background(Palette.ground)
        .preferredColorScheme(.dark)
        .task {
            guard let name = ReferenceLaunch.capture, ReferenceLaunch.exhibit != nil else { return }
            let screen = ScreenCapture()
            guard await screen.start() else { return }
            if let seconds = ReferenceLaunch.window {
                await screen.keepWindow(named: name, seconds: seconds)
            } else {
                try? await Task.sleep(for: .seconds(1.5))
                screen.save(named: name)
            }
            screen.stop()
        }
    }
}

/// The exhibits grouped by status, the chosen one marked.
struct ExhibitList: View {
    @Binding var chosen: Exhibit

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Liquid Glass").font(.system(size: 30, weight: .bold)).foregroundStyle(Palette.textHigh)
                    let ready = Exhibit.allCases.filter { $0.status == .ready }.count
                    Text("\(ready) of \(Exhibit.allCases.count) elements ready on Android")
                        .font(.system(size: 15)).foregroundStyle(Palette.textMedium)
                }
                .padding(.leading, 12)
                .padding(.bottom, 8)
                ForEach(ExhibitStatus.allCases, id: \.self) { status in
                    let group = Exhibit.allCases.filter { $0.status == status }
                    if !group.isEmpty {
                        Text(status.label.uppercased())
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Palette.textMedium)
                            .padding(.leading, 12)
                            .padding(.top, 20)
                            .padding(.bottom, 6)
                        ForEach(group) { exhibit in
                            Button {
                                chosen = exhibit
                            } label: {
                                HStack(spacing: 12) {
                                    Circle().fill(exhibit.status.color).frame(width: 8, height: 8)
                                    Text(exhibit.title)
                                        .font(.system(size: 16, weight: exhibit == chosen ? .semibold : .regular))
                                        .foregroundStyle(Palette.textHigh)
                                    Spacer(minLength: 0)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 11)
                                .background(
                                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                                        .fill(exhibit == chosen ? Palette.textHigh.opacity(0.12) : .clear)
                                )
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .padding(12)
        }
    }
}

/// An exhibit's name, status and summary over the stage where the system draws it.
struct ExhibitPage: View {
    let exhibit: Exhibit

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Text(exhibit.title).font(.system(size: 28, weight: .bold)).foregroundStyle(Palette.textHigh)
                Text(exhibit.status.label)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(exhibit.status.color)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(exhibit.status.color.opacity(0.16)))
            }
            Text(exhibit.summary).font(.system(size: 15)).foregroundStyle(Palette.textMedium)
            // The stage is the Android gallery's size, so an exhibit stands over the same content on both.
            ExhibitStage(exhibit: exhibit)
                .id(exhibit)
                .frame(width: StageSize.width, height: StageSize.height)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.top, 6)
        }
        .padding(16)
    }
}

/// The content glass stands over, block for block the Android sample's: a header, rows of posters, text and bands,
/// then the calibration patterns.
struct BackdropContent: View {
    var body: some View {
        LazyVStack(spacing: 0) {
            Header()
            ForEach(0..<12, id: \.self) { block in
                switch block % 4 {
                case 0: PosterRow(offset: block)
                case 1: TextBlock()
                case 2: Bands()
                default: PosterRow(offset: block + 3)
                }
            }
            CalibrationGrid()
            CalibrationRamp(horizontal: true, period: 48).frame(height: 160)
            CalibrationRamp(horizontal: false, period: 96).frame(height: 240)
            CalibrationProbe(horizontal: true).frame(height: 160)
            CalibrationProbe(horizontal: false).frame(height: 200)
        }
        .padding(.bottom, 140)
    }
}

/// The size of an exhibit's stage, in points, the Android gallery's in density-independent pixels.
let StageSize = CGSize(width: 400, height: 600)

/// Content that is hard for glass to stand over, scrolling under the exhibit.
struct StageBackdrop: View {
    var body: some View {
        ScrollView {
            BackdropContent()
        }
        .background(Palette.ground)
    }
}

/// An element standing over the stage's content, inset from its edges.
struct OverBackdrop<Content: View>: View {
    var alignment: Alignment = .center
    @ViewBuilder let content: Content

    var body: some View {
        ZStack(alignment: alignment) {
            StageBackdrop()
            content.padding(24)
        }
    }
}

extension View {
    /// Prints this view's frame in the window as `FRAME <name> x y width height`, in points, when the app was launched
    /// with `GLASS_FRAMES`, so a measurement reads the system's own geometry.
    /// - Parameter name: The name the line carries.
    /// - Returns: The view, reporting its frame.
    func reportsFrame(_ name: String) -> some View {
        onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { frame in
            if ProcessInfo.processInfo.environment["GLASS_FRAMES"] != nil {
                print("FRAME \(name) \(frame.minX) \(frame.minY) \(frame.width) \(frame.height)")
            }
            FrameLog.shared.record(name, frame)
        }
    }
}

/// Keeps the latest frame of every view that reports one in `<GLASS_CAPTURE>-frames.txt` of the app's Documents, one
/// `<name> x y width height` line each, in points, so a capture on a device finds the region to record.
final class FrameLog: @unchecked Sendable {
    /// The log of this launch.
    static let shared = FrameLog()

    private let lock = NSLock()
    private var frames: [String: CGRect] = [:]
    private let file = ProcessInfo.processInfo.environment["GLASS_CAPTURE"].map {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("\($0)-frames.txt")
    }

    /// Records `frame` as the latest frame of `name` and writes the log again.
    func record(_ name: String, _ frame: CGRect) {
        guard let file else { return }
        let text = lock.withLock {
            frames[name] = frame
            return frames.keys.sorted().map { key in
                let f = frames[key]!
                return "\(key) \(f.minX) \(f.minY) \(f.width) \(f.height)"
            }.joined(separator: "\n")
        }
        try? text.write(to: file, atomically: true, encoding: .utf8)
    }
}

/// A round glass control holding a glyph, the label of a menu or a button.
struct GlassDisc: View {
    let symbol: String
    var diameter: CGFloat = 56

    var body: some View {
        Image(systemName: symbol)
            .font(.system(size: diameter * 0.36, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: diameter, height: diameter)
            .glassEffect(.regular.interactive(), in: Circle())
    }
}

/// A short name on a capsule of glass, readable over any content.
struct GlassCaption: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .glassEffect(.regular, in: Capsule())
    }
}

/// The system's drawing of one exhibit.
struct ExhibitStage: View {
    let exhibit: Exhibit

    var body: some View {
        switch exhibit {
        case .material: OverBackdrop { MaterialStage() }
        case .glassButtons: OverBackdrop { GlassButtonsStage() }
        case .tabBar: OverBackdrop(alignment: .bottom) { TabBarStage() }
        case .segmentedControl: OverBackdrop { SegmentedStage() }
        case .joiningGlass: OverBackdrop { JoiningStage() }
        case .menus: OverBackdrop { MenusStage() }
        case .morphingPanel: OverBackdrop(alignment: .topLeading) { MorphingPanelStage() }
        case .contextMenu: OverBackdrop { ContextMenuStage() }
        case .submenus: OverBackdrop(alignment: .topTrailing) { SubmenusStage() }
        case .capsuleButtons: OverBackdrop { CapsuleButtonsStage() }
        case .toolbar: ToolbarStage()
        case .toggle: OverBackdrop { ToggleStage() }
        case .slider: OverBackdrop { SliderStage() }
        case .sheet: OverBackdrop { SheetStage() }
        case .alert: OverBackdrop { AlertStage() }
        case .popover: OverBackdrop { PopoverStage() }
        case .search: SearchStage()
        case .minimizingTabBar: MinimizingTabBarStage()
        case .bottomAccessory: BottomAccessoryStage()
        case .scrollEdge: ScrollEdgeStage()
        case .clearAndTinted: OverBackdrop { ClearAndTintedStage() }
        case .touchLight: OverBackdrop { TouchLightStage() }
        case .textMenu: OverBackdrop { TextMenuStage() }
        case .adaptiveSidebar: AdaptiveSidebarStage()
        }
    }
}

/// Still glass of several sizes, thin and thick, round and a capsule.
struct MaterialStage: View {
    var body: some View {
        HStack(spacing: 28) {
            ForEach([36, 51, 70, 100] as [CGFloat], id: \.self) { side in
                Color.clear.frame(width: side, height: side).glassEffect(.regular, in: Circle())
            }
            Color.clear.frame(width: 240, height: 62).glassEffect(.regular, in: Capsule())
        }
    }
}

/// Round glass buttons of three sizes.
struct GlassButtonsStage: View {
    var body: some View {
        HStack(spacing: 28) {
            ForEach([(46, "play.fill"), (56, "heart.fill"), (69, "square.and.arrow.up")] as [(CGFloat, String)], id: \.0) { diameter, symbol in
                Button {} label: { GlassDisc(symbol: symbol, diameter: diameter) }
                    .buttonStyle(.plain)
            }
        }
    }
}

/// A tablet's tab bar at the foot of the stage.
struct TabBarStage: View {
    @State private var held = 0

    var body: some View {
        ReferenceBar(held: $held)
    }
}

/// The glass track the Android sample copies, above the system's own segmented control.
struct SegmentedStage: View {
    @State private var held = 1

    var body: some View {
        VStack(spacing: 28) {
            DensityTrack(held: $held)
            Picker("Density", selection: $held) {
                ForEach(0..<3, id: \.self) { option in
                    Image(systemName: ["square.grid.3x3.fill", "square.grid.2x2.fill", "square.fill"][option]).tag(option)
                }
            }
            .pickerStyle(.segmented)
            .frame(width: 300)
        }
    }
}

/// Two circles of glass that move together until they join and apart again, over and over.
struct JoiningStage: View {
    @State private var together = false

    var body: some View {
        GlassEffectContainer(spacing: 24) {
            HStack(spacing: together ? -14 : 70) {
                Color.clear.frame(width: 90, height: 90).glassEffect(.regular, in: Circle())
                Color.clear.frame(width: 90, height: 90).glassEffect(.regular, in: Circle())
            }
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.8).repeatForever(autoreverses: true)) { together = true }
        }
    }
}

/// The four kinds of menu: titled sections of glyphed rows, choices among other rows, a plain menu and one that rises
/// from the foot of the stage.
struct MenusStage: View {
    @State private var order = 1
    @State private var ascending = true

    var body: some View {
        Color.clear
            .overlay(alignment: .topLeading) {
                Menu { LibraryMenuEntries() } label: { GlassDisc(symbol: "gearshape.fill") }
            }
            .overlay(alignment: .topTrailing) {
                Menu {
                    Section("Sort by") {
                        Picker("Sort by", selection: $order) {
                            Text("Title").tag(0)
                            Text("Year").tag(1)
                            Text("Added").tag(2)
                        }
                        .pickerStyle(.inline)
                        Toggle("Ascending", isOn: $ascending)
                    }
                    Section {
                        Button("Share", systemImage: "square.and.arrow.up") {}
                        Button("Unavailable", systemImage: "nosign") {}.disabled(true)
                        Button("Remove", systemImage: "trash", role: .destructive) {}
                    }
                } label: {
                    GlassDisc(symbol: "ellipsis")
                }
            }
            .overlay(alignment: .leading) {
                Menu {
                    Button("15 minutes") {}
                    Button("30 minutes") {}
                    Button("End of chapter") {}
                    Button("Off", role: .destructive) {}
                } label: {
                    GlassDisc(symbol: "moon.zzz")
                }
            }
            .overlay(alignment: .bottomLeading) {
                Menu {
                    Button("Collection", systemImage: "list.bullet") {}
                    Button("Message", systemImage: "envelope") {}
                    Button("Order", systemImage: "cart") {}
                } label: {
                    GlassDisc(symbol: "plus")
                }
            }
    }
}

/// A gear that opens into a panel as one piece of glass.
struct MorphingPanelStage: View {
    @State private var open = false
    @State private var blur = 0.5
    @State private var liquid = true
    @State private var reduceMotion = false
    @Namespace private var glass

    var body: some View {
        GlassEffectContainer(spacing: 24) {
            if open {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Text("Material").font(.system(size: 20, weight: .semibold))
                        Spacer()
                        Button("Done") { withAnimation(.bouncy) { open = false } }
                    }
                    Toggle("Liquid glass", isOn: $liquid)
                    Toggle("Reduce motion", isOn: $reduceMotion)
                    Text("Blur").foregroundStyle(.secondary)
                    Slider(value: $blur)
                }
                .padding(20)
                .frame(width: 340)
                .glassEffect(.regular, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
                .glassEffectID("panel", in: glass)
            } else {
                Button {
                    withAnimation(.bouncy) { open = true }
                } label: {
                    GlassDisc(symbol: "gearshape.fill")
                }
                .buttonStyle(.plain)
                .glassEffectID("panel", in: glass)
            }
        }
    }
}

/// Cards that open a context menu on a long press, one with a preview of its own.
struct ContextMenuStage: View {
    var body: some View {
        HStack(spacing: 32) {
            PosterCard(poster: posters[2])
                .reportsFrame("context.first")
                .accessibilityIdentifier("context.first")
                .contextMenu {
                    Button("Play", systemImage: "play.fill") {}
                    Button("Details", systemImage: "info.circle") {}
                    Button("Save", systemImage: "bookmark") {}
                    Button("Remove", systemImage: "trash", role: .destructive) {}
                }
            PosterCard(poster: posters[6])
                .reportsFrame("context.second")
                .accessibilityIdentifier("context.second")
                .contextMenu {
                    Button("Play", systemImage: "play.fill") {}
                    Button("Share", systemImage: "square.and.arrow.up") {}
                } preview: {
                    PosterCard(poster: posters[6]).scaleEffect(1.6).frame(width: 272, height: 408)
                }
        }
    }
}

/// A menu with a submenu and an entry carrying a second line.
struct SubmenusStage: View {
    var body: some View {
        Menu {
            Button {} label: {
                Label {
                    Text("Download")
                    Text("2.4 GB")
                } icon: {
                    Image(systemName: "arrow.down.circle")
                }
            }
            Menu("Share", systemImage: "square.and.arrow.up") {
                Button("Message", systemImage: "message") {}
                Button("Mail", systemImage: "envelope") {}
            }
            Menu("More", systemImage: "ellipsis.circle") {
                Button("First", systemImage: "1.circle") {}
                Button("Second", systemImage: "2.circle") {}
            }
        } label: {
            GlassDisc(symbol: "ellipsis")
        }
    }
}

/// Buttons of clear and of prominent glass, in three sizes.
struct CapsuleButtonsStage: View {
    var body: some View {
        VStack(spacing: 22) {
            HStack(spacing: 16) {
                Button {} label: { Text("Play").reportsFrame("play.label") }.buttonStyle(.glass).reportsFrame("play")
                Button {} label: {
                    Label { Text("Play").reportsFrame("playIcon.text") } icon: { Image(systemName: "play.fill").reportsFrame("playIcon.icon") }
                }
                .buttonStyle(.glass)
                .reportsFrame("playIcon")
                Button {} label: { Image(systemName: "heart.fill").reportsFrame("heart.label") }.buttonStyle(.glass).reportsFrame("heart")
                Button {} label: { Text("Small").reportsFrame("small.label") }.buttonStyle(.glass).controlSize(.small).reportsFrame("small")
                Button {} label: { Text("Mini").reportsFrame("mini.label") }.buttonStyle(.glass).controlSize(.mini).reportsFrame("mini")
            }
            HStack(spacing: 16) {
                Button("Buy") {}.buttonStyle(.glassProminent).reportsFrame("buy")
                Button("Download", systemImage: "arrow.down") {}.buttonStyle(.glassProminent).tint(Palette.accent).reportsFrame("download")
                Button("Delete", role: .destructive) {}.buttonStyle(.glassProminent).tint(.red).reportsFrame("delete")
            }
            HStack(spacing: 16) {
                Button {} label: { Text("Large").reportsFrame("large.label") }.buttonStyle(.glass).controlSize(.large).reportsFrame("large")
                Button {} label: { Text("Extra large").reportsFrame("xl.label") }.buttonStyle(.glass).controlSize(.extraLarge).reportsFrame("xl")
                Button("Prominent", systemImage: "star.fill") {}.buttonStyle(.glassProminent).controlSize(.extraLarge).reportsFrame("prominentXL")
            }
        }
    }
}

/// A page with its toolbar: grouped buttons at the top and at the bottom.
struct ToolbarStage: View {
    var body: some View {
        NavigationStack {
            StageBackdrop()
                .navigationTitle("Library")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Back", systemImage: "chevron.left") {}
                    }
                    ToolbarItemGroup(placement: .topBarTrailing) {
                        Button("Share", systemImage: "square.and.arrow.up") {}
                        Button("Favorite", systemImage: "heart") {}
                    }
                    ToolbarSpacer(.fixed, placement: .topBarTrailing)
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("More", systemImage: "ellipsis") {}
                    }
                    ToolbarItemGroup(placement: .bottomBar) {
                        Button("Shuffle", systemImage: "shuffle") {}
                        Spacer()
                        Button("Play", systemImage: "play.fill") {}
                    }
                }
        }
    }
}

/// Switches on and off.
struct ToggleStage: View {
    @State private var first = true
    @State private var second = false

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Toggle(isOn: $first) { GlassCaption(text: "Downloads") }.reportsFrame("toggle.on").accessibilityIdentifier("toggle.first")
            Toggle(isOn: $second) { GlassCaption(text: "Subtitles") }.reportsFrame("toggle.off").accessibilityIdentifier("toggle.second")
        }
        .frame(width: 280)
    }
}

/// A continuous slider and one with steps.
struct SliderStage: View {
    @State private var volume = 0.4
    @State private var rating = 3.0

    var body: some View {
        VStack(spacing: 28) {
            Slider(value: $volume).reportsFrame("slider").accessibilityIdentifier("slider.volume")
            Slider(value: $rating, in: 0...5, step: 1).reportsFrame("slider.steps").accessibilityIdentifier("slider.steps")
        }
        .frame(width: 352)
    }
}

/// A button that raises a sheet.
struct SheetStage: View {
    @State private var shown = false

    var body: some View {
        Button("Show sheet") { shown = true }
            .buttonStyle(.glass)
            .controlSize(.large)
            .sheet(isPresented: $shown) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Sheet").font(.title2.bold())
                    Text("Drag it up to full height to see it turn opaque.").foregroundStyle(.secondary)
                    Spacer()
                }
                .padding(24)
                .presentationDetents([.medium, .large])
            }
    }
}

/// A button that raises an alert.
struct AlertStage: View {
    @State private var shown = false

    var body: some View {
        Button("Show alert") { shown = true }
            .buttonStyle(.glass)
            .controlSize(.large)
            .reportsFrame("alert.show")
            .accessibilityIdentifier("alert.show")
            .alert("Remove this collection?", isPresented: $shown) {
                Button("Remove", role: .destructive) {}
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Its titles stay in the library.")
            }
    }
}

/// A button that opens a popover.
struct PopoverStage: View {
    @State private var shown = false

    var body: some View {
        Button("Show popover") { shown = true }
            .buttonStyle(.glass)
            .controlSize(.large)
            .popover(isPresented: $shown) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Popover").font(.headline)
                    Text("A panel that points at its control.").foregroundStyle(.secondary)
                }
                .padding(20)
                .frame(width: 280)
            }
    }
}

/// A tab bar laid out as on a phone, whose Search tab opens into a field.
struct SearchStage: View {
    @State private var text = ""

    var body: some View {
        TabView {
            ForEach(Array(entries.prefix(3).enumerated()), id: \.offset) { _, entry in
                Tab(entry.label, systemImage: entry.symbol) { StageBackdrop() }
            }
            Tab(role: .search) {
                NavigationStack { StageBackdrop().navigationTitle("Search") }
                    .searchable(text: $text)
            }
        }
        .environment(\.horizontalSizeClass, .compact)
    }
}

/// A tab bar laid out as on a phone that shrinks while the content scrolls down.
struct MinimizingTabBarStage: View {
    var body: some View {
        TabView {
            ForEach(Array(entries.prefix(4).enumerated()), id: \.offset) { _, entry in
                Tab(entry.label, systemImage: entry.symbol) { StageBackdrop() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .environment(\.horizontalSizeClass, .compact)
    }
}

/// A tab bar laid out as on a phone with a player's strip above it.
struct BottomAccessoryStage: View {
    var body: some View {
        TabView {
            ForEach(Array(entries.prefix(4).enumerated()), id: \.offset) { _, entry in
                Tab(entry.label, systemImage: entry.symbol) { StageBackdrop() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .tabViewBottomAccessory {
            HStack(spacing: 12) {
                Image(systemName: "music.note")
                Text("Now playing").font(.system(size: 15, weight: .semibold))
                Spacer()
                Image(systemName: "play.fill")
                Image(systemName: "forward.fill")
            }
            .padding(.horizontal, 16)
        }
        .environment(\.horizontalSizeClass, .compact)
    }
}

/// A page whose content fades under its bars, softly or with a hard edge.
struct ScrollEdgeStage: View {
    @State private var hard = false

    var body: some View {
        NavigationStack {
            StageBackdrop()
                .scrollEdgeEffectStyle(hard ? .hard : .soft, for: .all)
                .navigationTitle("Scroll edge")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        Picker("Edge", selection: $hard) {
                            Text("Soft").tag(false)
                            Text("Hard").tag(true)
                        }
                        .pickerStyle(.segmented)
                        .frame(width: 200)
                    }
                    ToolbarItemGroup(placement: .bottomBar) {
                        Button("Shuffle", systemImage: "shuffle") {}
                        Spacer()
                        Button("Play", systemImage: "play.fill") {}
                    }
                }
        }
    }
}

/// Regular, clear and tinted glass side by side.
struct ClearAndTintedStage: View {
    var body: some View {
        // Two by two, so the four stand inside the stage.
        Grid(horizontalSpacing: 28, verticalSpacing: 28) {
            GridRow {
                sample(.regular, "Regular")
                sample(.clear, "Clear")
            }
            GridRow {
                sample(.regular.tint(Palette.accent), "Tinted")
                sample(.clear.tint(Color(argb: 0x6629_79FF)), "Clear, tinted")
            }
        }
    }

    private func sample(_ glass: Glass, _ name: String) -> some View {
        VStack(spacing: 12) {
            Color.clear.frame(width: 100, height: 100).glassEffect(glass.interactive(), in: Circle())
                .accessibilityIdentifier("variant.\(name)")
            GlassCaption(text: name)
        }
    }
}

/// A wide pane of interactive glass to press and drag across.
struct TouchLightStage: View {
    var body: some View {
        Text("Press and drag")
            .font(.system(size: 20, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: 440, height: 260)
            .glassEffect(.regular.interactive(), in: RoundedRectangle(cornerRadius: 32, style: .continuous))
            .accessibilityIdentifier("exhibit.touch")
    }
}

/// Text to select, which raises the system's text menu.
struct TextMenuStage: View {
    @State private var text = "Select a word of this text to raise the menu of cut, copy and paste."

    var body: some View {
        TextEditor(text: $text)
            .font(.system(size: 20))
            .scrollContentBackground(.hidden)
            .padding(16)
            .frame(width: 440, height: 200)
            .glassEffect(.regular, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

/// A sidebar of glass over a page.
struct AdaptiveSidebarStage: View {
    var body: some View {
        NavigationSplitView {
            List(entries) { entry in
                Label(entry.label, systemImage: entry.symbol)
            }
            .navigationTitle("Library")
        } detail: {
            StageBackdrop()
        }
    }
}
