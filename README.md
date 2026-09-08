# Gelato

[![DOI](https://zenodo.org/badge/1271120878.svg)](https://doi.org/10.5281/zenodo.22644100)

Gelato (former GelAnno or wbtools) is a Fiji/ImageJ plugin for traceable gel-figure assembly and paired kDa values mapping
Supported input: tif/tiff, png, jpg/jpeg

Installation for windows: put gelato-1.0.0.jar file in "plugins" folder of Fiji and enjoy!

Installation for Mac users: open the file contents of Fiji by right-click. 
Drag gelato-1.0.0.jar to "plugins" directory. 
Open Fiji, choose "Plugins" from dropdown menu, select "Gelato".

Installation through Fiji Update site: 
Open Fiji. Click Help>Update..., then "Manage update sites". Click "Add Unlisted Site".
Fill the columns Name: GelAnno, URL: https://sites.imagej.net/GelAnno/.
Apply changes. Fiji needs to be restarted to finilize the update.
More about how to follow an update site: https://imagej.net/update-sites/following

Works the same in ImageJ if iText module is present (used for PDF export).







# Basic workflow for separate marker/blot images

1. Click **Open kDa Marker Image...** and choose the ladder/marker (colorimetric) image.
2. Click each band in the marker image, and enter its label.
3. Click **Open Gel Image...** and choose the blot image.
4. Select crop for the figure by dragging, then press **Crop Region -> Figure**. The markers are transferred into the crop's local coordinates and included
   in every crop whose vertical range (in crop's local coordinates) contains them.

# Basic workflow for merged marker/blot images

1. Click **Open Gel Image...** and choose the merged image.
2. Click each band in the marker image, and enter its label.
3. Click **Stop marking kDa bands**.
4. Select crop for the figure by dragging, then press **Crop Region -> Figure**. The markers are transferred into the crop's local coordinates and included
   in every crop whose vertical range (in crop's local coordinates) contains them.

# Image reconstruction 

When the crops appeared on canvas, lick "Show Coordinate Log" to view/copy used sources and coordinates. 

The log is used to reconstruct the figure from declaired sources and to highlight the used crops and marked kDa bands. 
Click "Reconstruct from Log..." and paste or load a log file ("Load Log..."). Click "Reconstruct". The program will prompt to load the declared images from the log file one by one. 
After all images are loaded, GelAnno will reconstruct the figure side to side with highlighted markers and cropped areas on original images.

More information on coordinate log format in How it works.


# Adding annotations

After adding at least one crop on the A4 canvas, you can add figure annotations. Annotations are not logged or reconstructed. 

1. **Add sample labels**
Click anywhere on a crop to put a sample label above.
2. **Add band ticks**
Click anywhere on a crop and enter a band-specific label. The tick with the label appears on the right edge of the crop on the corresponding coordinate.
3. **Draw H-/V-line**
Draw horizontal or vertical lines by dragging.
4. **Edit Annotations**
Select any annotation exept for kDa markers to move, edit, resize text or delete. 



# Export

Exports the A4 cavnas to PDF. Everything (exept for the raster images) remains vector for publication or further editing. 

When **Use clip groups to group crop bands with their annotations** is selected during PDF export, the Form XObject structure is:

```text
Page
|- Band 1 Form
|  |- Raster crop image
|  |- Vector boundary
|  |- MW Markers Form
|     |- Ticks Form
|     `- kDa Values Form
|  |- Sample Labels Form
|  `- Band Ticks Form
|- Band 2 Form
|- Band Names Form
`- Free H/V line objects
```

This keeps every crop, its boundary, its sample labels, and its MW annotations under one PDF graphical object while keeping the band names separate. Illustrator imports the required Form XObject bounds as clipping masks; the symmetric bounds allow MW annotations to move between the left and right sides of a crop without being clipped. Leave the option unchecked to export the original flat structure with independently selectable objects and no band-level clip groups.



# How it works
Very simple.

Paired marker-blot Molecular weight annotation:
When kDa markers are registered by clicking on Marker image, the program stores its absolute coordinates (Xabs, Yabs) and the values, creating a "kDa markers set". 
When paired blot image is loaded, the kDa markers are shown on loaded image using the stored information.
Each crop of the blot image has its own local coordinate system that can be tilted relative to the original image. 
kDa markers that are falling into the crop's vertical boundaries in its local coordinate system are assigned to the crop with local coordinates (y_in_gel). 
When the crop is added to A4 canvas, Mw ticks are plotted on its edge using y_in_crop. 
If a new Marker image is loaded, new "kDa markers set" is created and applied to Gel images loaded afterwards.

Figure reconstruction:
Coordinate log is a human-readable text file with coordinate and source information of all used kDa marker sets and crops. 
Together with original images, it can be used to reconstruct the crops and kDa ticks, highlighting their positions on the images for facile audit of published figures.

Structure:
- **Format information:** log-format and plugin versions.
- **Coordinate convention:** the origin is the top-left pixel; X increases to the right, Y increases downward, and angles are measured clockwise.
- **kDa marker sets:** the source image, image dimensions, and absolute coordinates and labels of each marker. Only marker sets used by crops in the current figure are included.
- **Crops:** their order and band names, source image and dimensions, crop size, angle, origin, and all four corner coordinates.
- **Marker mapping:** the marker set used by each crop, coordinate scaling between images, and the exact position of every kDa tick within the crop.
- **Compatibility warnings:** shown when marker and gel images have different dimensions or aspect ratios.

In principle, anyone can create the log file manually. GelAnno can reconstruct the images with either 3 corners OR size, angle, and origin of the crops, allowing for 1 pixel tolerance.
kDa information is optional (Global kDa marker sets, Used kDa markers). If used markers per band are not specified, all markers that apply are reconstructed by default.

# Implemented:

- Open TIFF, PNG, JPEG images and convert them to RGB for consistent rendering.
- Toggle kDa marker mode, click the source image, enter a kDa label, and store absolute image coordinates.
- Mirror kDa markers onto the Gel image, including synchronized label visibility, undo, and clear behavior.
- Switch safely between separate-marker-image and direct-on-Gel marker workflows.
- Preserve versioned kDa marker sets once crops use them.
- Show, copy, and save a coordinate log containing source paths, crop corners, marker mappings, and compatibility warnings. Only marker sets referenced by crops in the current figure are included.
- Reconstruct figures from pasted or loaded coordinate logs and user-supplied source images.
- Open one annotated source-image copy per reconstructed kDa marker set.
- Open each selected raw blot image once during reconstruction and overlay all of its crop boundaries, band labels, mapped marker coordinates, and crop-local connector lines to the left-edge tick positions. kDa labels move to the side of each X opposite its connectors.
- Export reconstructed annotated source windows as a full-resolution lossless PNG or as a hybrid PDF containing the source image as raster data and all visible overlay lines and text as vector objects.
- Draw an ordinary or rotated crop ROI in ImageJ/Fiji and add the cropped blot to a figure canvas.
- Project kDa marker coordinates into the rotated crop coordinate system, so tilted crops keep the molecular-weight ticks aligned.
- Show kDa ticks and labels on the left edge of each crop.
- Compose the figure on a fixed A4 portrait artboard, shown against a gray pasteboard with Fit, slider, +/- and Ctrl+mouse-wheel zoom controls. Zoom changes only the view.
- Drag crops on the figure canvas and resize the selected crop with the paired Narrower/Wider controls; ticks move and scale with the crop.
- Add crop-attached sample labels with a remembered session angle and font size. Horizontal labels center on the clicked lane; tilted labels begin at the clicked lane and rise to the right.
- Add crop-attached band ticks on the right edge. Their text can be edited and resized, and the complete tick can be moved vertically while remaining attached to its crop.
- Draw free horizontal and vertical vector lines by dragging. In Edit Annotations mode their bodies move and their endpoint handles change their length. Lines can be copied, pasted beside the original at the same height, and deleted.
- Select text through its visible bounds in Edit Annotations mode, including rotated sample labels; Shift-click supports multiple selection, and movable text can be dragged or nudged with the keyboard.
- Delete user-created sample labels, band ticks, band names, and free lines with Delete or Backspace. Calibrated kDa values and ticks remain protected.
- Resize selected text with A-/A+, or resize all figure text and future defaults when no text is selected. MW values are selectable for font resizing only. Font sizes are genuine typographic points on screen and in PDF.
- Export an A4 portrait PDF either as separate editable objects (the default) or with an optional Form XObject per band containing the raster crop, vector boundary, crop-attached sample labels, and a nested MW-marker Form XObject with separate tick and kDa-value forms. Raster DPI changes only embedded crop resolution; page geometry and vector text stay fixed. Group bounds include editing room on both sides of the crop, and neither mode paints a page background.
Annotation edits are runtime figure state in 0.8.0. They are included in PDF export but intentionally are not written to or reconstructed from coordinate logs.
New coordinate logs use the `GelAnno Coordinate Log` header. GelAnno also accepts the legacy `WB Tool Coordinate Log` header, so logs saved by earlier Java versions remain reconstructable without changing log format version 1.

# Citation

If you use Gelato to prepare figures for a publication, please cite using DOI above.

Maria A. Pirozhkova, Elisheva Babitz. Gelato: an ImageJ/Fiji plugin for traceable gel figures assembly and annotation. GitHub repository: https://github.com/masha-rgfj/Gelato-fiji-plugin 2026



## Contributions

- Maria A. Pirozhkova — concept, software development, implementation, maintenance
- Elisheva Babitz — documentation, user manuals, installation support, testing, user onboarding, and dissemination


# Licence
AGPL-3

# Contact
pirozhkova@mail.tau.ac.il
