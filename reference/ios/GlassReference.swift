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
            } else {
                ReferenceScreen()
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
    /// Whether each probe circle carries a glyph in the primary style beside one in explicit white.
    static let probeInk = ProcessInfo.processInfo.environment["GLASS_PROBE_INK"] != nil
    /// The number of presses a recording keeps a clip of before it ends.
    static let taps = Int(value("GLASS_TAPS") ?? 4)
    /// Whether the app shows the system's own glass controls, a tab bar and a segmented control, instead of the screen.
    static let controls = ProcessInfo.processInfo.environment["GLASS_CONTROLS"] != nil
    /// Whether the system's controls stand over a white page instead of the screen's content.
    static let controlsOverWhite = ProcessInfo.processInfo.environment["GLASS_CONTROLS_WHITE"] != nil
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
            .scrollPosition($position)
            .background(Palette.ground)
            .ignoresSafeArea()

            VStack {
                HStack(spacing: 12) {
                    Menu {
                        Button("Liquid Glass", systemImage: "drop.fill") {}
                        Button("Reduce motion", systemImage: "figure.walk") {}
                        Button("Reset", systemImage: "arrow.counterclockwise") {}
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(Palette.textHigh)
                            .frame(width: 56, height: 56)
                    }
                    .buttonStyle(.glass)
                    .buttonBorderShape(.circle)
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
            LinearGradient(
                colors: [Color(argb: 0xFF3A_0CA3), Color(argb: 0xFFF7_2585), Color(argb: 0xFFFF_D60A)],
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
    BarEntry(label: "Film", symbol: "play.fill"),
    BarEntry(label: "Serie", symbol: "star.fill"),
    BarEntry(label: "Documentari", symbol: "info.circle.fill"),
    BarEntry(label: "Altro", symbol: "ellipsis"),
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

                face(BarEntry(label: "Cerca", symbol: "magnifyingglass"), selected: false, showsLabel: false)
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
                                .glassEffect(interactive ? .regular.interactive() : .regular, in: Circle())
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
