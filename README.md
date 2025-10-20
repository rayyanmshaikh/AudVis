Android Kotlin app to dynamically visualize output audio from a device on the edge of the screen. Dynamically matches the colour and size to the genre, volume and more.

## Features

- **Output Audio Capture**: Uses MediaProjection API to capture system audio output (Android 10+)
- **Edge Visualization**: Display audio visualization on screen edges
- **Multi-Edge Support**: Choose to display visualizer on any combination of edges (left, right, top, bottom)
- **Real-time Animation**: Smooth 60fps visualization with fade-out effects
- **Customizable Settings**: User-configurable edge selection via Settings page

## Settings

The app includes a Settings page accessible from the main screen where you can:

- **Select Edges**: Choose which screen edges to display the visualizer (minimum 1, maximum 4)

Settings are saved and will take effect the next time you start the visualizer.
