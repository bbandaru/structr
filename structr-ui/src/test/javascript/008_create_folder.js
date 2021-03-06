/*
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
var s = require('../setup'),
	login = require('../templates/login');

var testName = '008_create_folder';
var heading = "Create Folder", sections = [];
var desc = "This animation shows how to create a new folder.";
var numberOfTests = 3;

s.startRecording(window, casper, testName);

casper.test.begin(testName, numberOfTests, function(test) {

	casper.start(s.url);

	login.init(test, 'admin', 'admin');

	sections.push('Click on the "Files" menu entry.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: "#files_", wait: 1000});
	});

	sections.push('Click the "Add Folder" icon.');

	casper.then(function() {
		s.moveMousePointerAndClick(casper, {selector: ".add_folder_icon", wait: 2000});
	});

	sections.push('A new folder with a random name has been created in the folders area.');

	casper.then(function() {
		test.assertElementCount('#files-table .node.folder', 1);
	});

	casper.then(function() {
		s.animateHtml(testName, heading, sections);
	});

	casper.run();

});