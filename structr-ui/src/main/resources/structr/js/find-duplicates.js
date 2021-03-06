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
var _DuplicateFinder = new (function () {

	/* ~~~~~~~~~~ public API ~~~~~~~~~~ */
	 this.openDuplicateFinderDialog = function () {
		_init();

		Structr.dialog('Find Duplicates', _closeDialog, _closeDialog);
		_dialogIsOpen = true;

		_Logger.log(_LogType.FIND_DUPLICATES, "opened duplicate finder dialog");
		_showWaitElement();

		Command.findDuplicates(function(data) {
			_handleDuplicatesList(data);

			dialogText.append('<div id="no-duplicates-found"><img src="' + _Icons.tick_icon + '"> No more duplicates to show</div>');
		});

	};

	this.isDialogOpen = function () {
		return _dialogIsOpen;
	};

	this.reactToUpdateNotification = function (obj) {

		if (this.isDialogOpen()) {

			var $tr = $('tr.dup_' + obj.id, dialogText);

			if ($tr && $tr.length) {

				var $input = $tr.find('.duplicate-obj-edit-name input');

				if (obj.name !== $input.val()) {
					$input.val(obj.name);
					blinkGreen($input);
				}

				_checkTableForConflicts($tr.closest('table'));

			}

		}
	};

	this.reactToDeleteNotification = function (id) {

		if (this.isDialogOpen()) {

			var $tr = $('tr.dup_' + id, dialogText);

			if ($tr && $tr.length) {

				_deleteRowCallback($tr);

			}

		}
	};

	/* ~~~~~~~~~~ private fields ~~~~~~~~~~ */
	var _didInit      = false;
	var _dialogIsOpen = false;
	var _waitElement  = undefined;


	/* ~~~~~~~~~~ private methods ~~~~~~~~~~ */
	function _init() {
		if (_didInit === false) {
			$(document).on('click', '.remove-duplicate-free-table-icon', function (e) {
				_removeTable($(this).closest('table'));
			});

			$(document).on('click', '.ignore-duplicates-table-icon', function (e) {
				_removeTable($(this).closest('table'));
			});

			$(document).on('blur', '.duplicate-obj-edit-name input', function (e) {
				var $input = $(this);
				var fileId = $input.data('fileId');
				var newName = $input.val();
				var currentName = $input.data('currentName');

				if (currentName !== newName) {

					Command.setProperty(fileId, 'name', newName, undefined, function (data) {
						blinkGreen($input);
						$input.data('currentName', data.name);

						_checkTableForConflicts($input.closest('table'));
					});

				}
			});

			$(document).on('click', '.duplicate-obj-delete-file', function (e) {
				$(this).hide().next().show();
			});

			$(document).on('click', '.duplicate-obj-delete-file-no', function (e) {
				$(this).parent().hide().prev().show();
			});

			$(document).on('click', '.duplicate-obj-delete-file-yes', function (e) {
				var $btn = $(this);
				var objId = $btn.data('objId');
				var $tr = $btn.closest('tr');

				// we only get the broadcast - that is why we set up a callback and call it from the model...
				$tr.data('callback', function () {
					_deleteRowCallback($tr);
				});

				Command.deleteNode(objId);

			});

		}

		_didInit = true;
	};

	function _deleteRowCallback ($tr) {
		$tr.data('callback', null);
		var $table = $tr.closest('table');
		$tr.remove();

		_checkTableForConflicts($table);
	};

	function _closeDialog() {
		_Logger.log(_LogType.FIND_DUPLICATES, "closed opened duplicate finder dialog");
		_dialogIsOpen = false;
	};

	function _getWaitElement() {
		if (!_waitElement) {
			dialogText.append('<div id="searching-duplicates-wait"></div>');
			_waitElement = $('#searching-duplicates-wait', dialogText);
			Structr.loaderIcon(_waitElement, { marginBottom: '-6px' });
			_waitElement.append(' Searching for duplicates. Depending on the number of files this might take a while');
		}

		return _waitElement;
	};

	function _showWaitElement() {
		_getWaitElement().show();
	};

	function _handleDuplicatesList(data) {

		if (_dialogIsOpen) {

			_Logger.log(_LogType.FIND_DUPLICATES, "processing list of duplicate files", data);

			_getWaitElement().hide();

			var lastFile = {};
			var lastPath = undefined;
			var table;
			var tbody;

			data.forEach(function (file) {

				if (lastPath === undefined || lastPath !== file.path) {

					table = _createNewTable(file);
					tbody = table.children('tbody');
					dialogText.append(table);
				}

				_appendFileToTbody(file, tbody);

				lastPath = file.path;
				lastFile = file;
			});

			dialogText.append(table);

		} else {

			_Logger.log(_LogType.FIND_DUPLICATES, "IGNORING list of duplicate files - user already closed the dialog");

		}

	};

	function _createNewTable(obj) {

		var ignoreButton = '<button class="ignore-duplicates-table-icon button"><img src="' + _Icons.grey_cross_icon + '"> Ignore this time</button>';
		var noConflictsRemoveTableButton = '<button class="remove-duplicate-free-table-icon button"><img src="' + _Icons.tick_icon + '"> No more conflicts, remove table</button>';

		var table = $('<table class="duplicates-table props"><thead>'
				+ '<tr class="heading"><th colspan="7">' + obj.path + ' ' + ignoreButton + noConflictsRemoveTableButton + '</th></tr>'
				+ '<tr><th>ID</th><th>Name</th><th>Type</th><th>Content-Type</th><th>Size</th><th>Checksum</th><th>Actions</th></tr>'
				+ '</thead><tbody></tbody></table>');

		_setTableHasConflicts(table, true);

		return table;
	};

	function _appendFileToTbody(obj, tbody) {

		var fileLink = '';
		var extraColumns = '';
		var delAction = '<button class="duplicate-obj-delete-file"><img src="' + _Icons.delete_icon + '"> Delete</button><div style="display: none; white-space: nowrap;"><button class="duplicate-obj-delete-file-yes"><img src="' + _Icons.delete_icon + '"> Really Delete</button><button class="duplicate-obj-delete-file-no"><img src="' + _Icons.cross_icon + '"> Cancel</button></div>';

		if (obj.isFolder) {
			fileLink = obj.id;
			extraColumns = '<td colspan="3" class="placeholderText">not applicable</td><td></td>';
		} else {
			fileLink = '<a target="_blank" href="/' + obj.id + '">' + obj.id + '</a>';
			extraColumns = '<td>' + (obj.contentType || '') + '</td><td>' + obj.size + '</td><td>' + obj.checksum + '</td><td>' + delAction + '</td>';
		}

		var tr = $('<tr class="dup_' + obj.id + '"><td>' + fileLink  + '</td><td class="duplicate-obj-edit-name"><input type="text" value="' + obj.name + '"></td><td>' + obj.type + '</td>' + extraColumns + '</tr>');
		tbody.append(tr);

		tr.find('.duplicate-obj-edit-name input').data('fileId', obj.id);
		tr.find('.duplicate-obj-edit-name input').data('currentName', obj.name);
		tr.find('.duplicate-obj-delete-file-yes').data('objId', obj.id);

	};

	function _checkTableForConflicts($table) {

		var hasConflicts = true;
		var $inputs = $table.find('.duplicate-obj-edit-name input');

		if ($inputs.length <= 1) {

			hasConflicts = false;

		} else {

			var uniqueNames = $.unique($.makeArray($inputs).map(function (inp) { return $(inp).val(); }));

			if ($inputs.length === uniqueNames.length) {
				hasConflicts = false;
			}
		}

		_setTableHasConflicts($table, hasConflicts);

	};

	function _setTableHasConflicts($table, hasConflicts) {

		if (hasConflicts) {

			$table.find('.remove-duplicate-free-table-icon').hide();
			$table.find('.ignore-duplicates-table-icon').show();

		} else {

			$table.find('.ignore-duplicates-table-icon').hide();
			$table.find('.remove-duplicate-free-table-icon').show();

		}

	};

	function _removeTable($table) {

		$table.remove();

	};

});