/**
 * 
 * Copyright (c) 2014 Jonathan Wohl, Sustainable Engineering Lab, Columbia University
 * 
 * See the file LICENSE for copying permission.
 * 
 */
package edu.columbia.sel.grout.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.osmdroid.tileprovider.util.StreamUtils;

public class FolderZipper {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static void zipFolderToFile(final File pDestinationFile, final File pFolderToZip){
		try {
			//create ZipOutputStream object
			final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(pDestinationFile));
			final String baseName = pFolderToZip.getParent();
			
			addFolderToZip(pFolderToZip, out, baseName);

//			StreamUtils.closeStream(out);
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


	private static void addFolderToZip(final File folder, final ZipOutputStream zip, final String baseName) throws IOException {
		final File[] files = folder.listFiles();
		/* For each child (subdirectory/child-file). */
		for (final File file : files) {
			if (file.isDirectory()) {
				/* If the file is a folder, do recursrion with this folder.*/
				addFolderToZip(file, zip, baseName);
			} else {
				/* Otherwise zip it as usual. */
				String name;
				
				if (baseName == null) {
					name = file.getAbsolutePath();
				} else {
					name = file.getAbsolutePath().substring(baseName.length());					
				}
				
				final ZipEntry zipEntry = new ZipEntry(name);
				zip.putNextEntry(zipEntry);
				final FileInputStream fileIn = new FileInputStream(file);
				StreamUtils.copy(fileIn, zip);
//				StreamUtils.closeStream(fileIn);
				fileIn.close();
				zip.closeEntry();
			}
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
