document.addEventListener('DOMContentLoaded', async function () {
  const app = window.CampusVoice;
  const user = app.checkPageRole('admin');
  if (!user) return;

  app.wireCommon();
  document.getElementById('adminName').textContent = user.name;
  document.getElementById('adminDepartment').textContent = (user.department || '') + ' Department';

  const list = document.getElementById('adminComplaintList');
  const empty = document.getElementById('adminEmpty');
  let activeId = '';
  let currentComplaints = [];

  const nextAllowed = {
    PENDING: ['ASSIGNED'],
    ASSIGNED: ['IN_PROGRESS'],
    IN_PROGRESS: ['RESOLVED', 'REJECTED'],
    RESOLVED: [],
    REJECTED: []
  };

  function renderSummary(complaints) {
    const open = complaints.filter(function (item) { return !['RESOLVED', 'REJECTED'].includes(item.status); });
    document.getElementById('openPriorityCount').textContent = open.filter(function (item) { return ['HIGH', 'CRITICAL'].includes(item.priority); }).length;

    const statusCounts = ['PENDING', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'].map(function (value) {
      return {
        label: value.replace('_', ' '),
        count: complaints.filter(function (item) { return item.status === value; }).length,
        color: 'blue'
      };
    });

    const categoryCounts = app.categories.map(function (value) {
      return {
        label: value,
        count: complaints.filter(function (item) { return item.category === value; }).length,
        color: 'light'
      };
    }).filter(function (item) { return item.count > 0; });

    function bars(title, rows, total) {
      const maximum = Math.max.apply(null, rows.map(function (row) { return row.count; }).concat([1]));
      return '<section class="summary-card card"><div class="summary-title"><h2>' + title + '</h2><span>' + total + ' total</span></div>' +
        (rows.length ? rows.map(function (row) {
          return '<div class="bar-row"><div><span>' + app.escapeText(row.label) + '</span><strong>' + row.count + '</strong></div><span class="bar-track"><span class="bar-fill bar-' + row.color + '" style="width:' + Math.max(3, row.count / maximum * 100) + '%"></span></span></div>';
        }).join('') : '<p class="muted summary-empty">No complaints to summarize yet.</p>') +
        '</section>';
    }

    document.getElementById('summaryGrid').innerHTML = bars('By status', statusCounts, complaints.length) + bars('By category', categoryCounts, complaints.length);
  }

  async function fetchDepartmentComplaints(search, status, priority) {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (status) params.append('status', status);
    if (priority) params.append('priority', priority);

    const queryStr = params.toString() ? '?' + params.toString() : '';
    return await app.apiFetch('/admin/complaints' + queryStr);
  }

  async function render() {
    const search = document.getElementById('complaintSearch').value.trim();
    const status = document.getElementById('statusFilter').value;
    const priority = document.getElementById('priorityFilter').value;

    try {
      currentComplaints = await fetchDepartmentComplaints(search, status, priority);
      if (!Array.isArray(currentComplaints)) currentComplaints = [];
    } catch (err) {
      console.error('Failed to load admin complaints:', err);
      currentComplaints = [];
    }

    renderSummary(currentComplaints);

    document.getElementById('adminComplaintCount').textContent = currentComplaints.length + ' shown';
    empty.hidden = currentComplaints.length > 0;

    list.innerHTML = currentComplaints.map(function (item) {
      const highlight = ['HIGH', 'CRITICAL'].includes(item.priority) ? ' complaint-card-priority' : '';
      const terminal = ['RESOLVED', 'REJECTED'].includes(item.status);
      return '<article class="complaint-card' + highlight + '">' +
        '<div class="complaint-card-top">' +
        '<div>' +
        '<div class="card-id-row">' +
        '<span class="id-cell">' + app.escapeText(item.id) + '</span>' +
        '<span class="priority-badge priority-' + item.priority.toLowerCase() + '">' + app.escapeText(item.priority) + '</span>' +
        '<span class="status-badge status-' + app.statusClass(item.status) + '">' + app.escapeText(item.status.replace('_', ' ')) + '</span>' +
        '</div>' +
        '<h3>' + app.escapeText(item.title) + '</h3>' +
        '</div>' +
        '<span class="card-date">' + app.escapeText(app.formatDate(item.createdAt)) + '</span>' +
        '</div>' +
        '<p class="complaint-preview">' + app.escapeText(item.description) + '</p>' +
        '<div class="complaint-card-foot">' +
        '<div class="card-facts">' +
        '<span>' + app.escapeText(item.category) + '</span>' +
        '<span>From ' + app.escapeText(item.ownerName) + '</span>' +
        '</div>' +
        '<div class="card-actions">' +
        '<button class="text-button" type="button" data-details="' + app.escapeText(item.id) + '">View details</button>' +
        '<button class="button ' + (terminal ? 'button-muted' : 'button-outline') + '" type="button" data-update="' + app.escapeText(item.id) + '" ' + (terminal ? 'disabled' : '') + '>' +
        (terminal ? 'Closed' : 'Update Status') +
        '</button>' +
        '</div>' +
        '</div>' +
        '</article>';
    }).join('');

    list.querySelectorAll('[data-update]').forEach(function (button) {
      button.addEventListener('click', function () { openUpdate(button.dataset.update); });
    });
    list.querySelectorAll('[data-details]').forEach(function (button) {
      button.addEventListener('click', function () { showDetails(button.dataset.details); });
    });
  }

  function getComplaint(id) {
    return currentComplaints.find(function (item) { return item.id === id; });
  }

  async function showDetails(id) {
    let item = getComplaint(id);
    try {
      const fresh = await app.apiFetch('/complaints/' + encodeURIComponent(id));
      if (fresh) item = fresh;
    } catch (e) {}
    if (!item) return;

    document.getElementById('adminDetailTitle').textContent = item.title;
    document.getElementById('adminDetailContent').innerHTML =
      '<div class="detail-meta">' +
      '<span class="id-cell">' + app.escapeText(item.id) + '</span>' +
      '<span class="priority-badge priority-' + item.priority.toLowerCase() + '">' + app.escapeText(item.priority) + '</span>' +
      '<span class="status-badge status-' + app.statusClass(item.status) + '">' + app.escapeText(item.status.replace('_', ' ')) + '</span>' +
      '</div>' +
      '<dl class="detail-grid">' +
      '<div><dt>Category</dt><dd>' + app.escapeText(item.category) + '</dd></div>' +
      '<div><dt>Department</dt><dd>' + app.escapeText(item.department) + '</dd></div>' +
      '<div><dt>Submitted</dt><dd>' + app.escapeText(new Date(item.createdAt).toLocaleString()) + '</dd></div>' +
      '<div><dt>Submitted by</dt><dd>' + app.escapeText(item.ownerName) + '</dd></div>' +
      '</dl>' +
      '<div class="detail-description"><h3>Description</h3><p>' + app.escapeText(item.description).replace(/\n/g, '<br>') + '</p></div>' +
      '<h3 class="timeline-heading">Status timeline</h3>' + app.timelineMarkup(item.audit);

    document.getElementById('adminDetails').showModal();
  }

  function openUpdate(id) {
    const item = getComplaint(id);
    if (!item || !nextAllowed[item.status] || !nextAllowed[item.status].length) return;

    activeId = id;
    document.getElementById('updateHeading').textContent = 'Update ' + item.id;
    document.getElementById('updateComplaintSummary').innerHTML =
      '<strong>' + app.escapeText(item.title) + '</strong><span>Current status: ' + app.escapeText(item.status.replace('_', ' ')) + '</span>';

    app.fillOptions(document.getElementById('nextStatus'), nextAllowed[item.status], 'Select a new status');
    document.getElementById('updateRemarks').value = '';
    app.showError(document.getElementById('updateError'), '');
    document.getElementById('updateDialog').showModal();
  }

  document.getElementById('updateForm').addEventListener('submit', async function (event) {
    event.preventDefault();
    const error = document.getElementById('updateError');
    app.showError(error, '');

    const newStatus = document.getElementById('nextStatus').value;
    const remarks = document.getElementById('updateRemarks').value.trim();

    if (!newStatus) {
      return app.showError(error, 'Please select a new status.');
    }
    if (['RESOLVED', 'REJECTED'].includes(newStatus) && !remarks) {
      return app.showError(error, 'Remarks are required to resolve or reject a complaint.');
    }

    try {
      await app.apiFetch('/admin/complaints/' + encodeURIComponent(activeId) + '/status', {
        method: 'PUT',
        body: { newStatus, remarks }
      });
      document.getElementById('updateDialog').close();
      await render();
    } catch (err) {
      app.showError(error, err.message || 'Failed to update status.');
    }
  });

  let debounceTimer = null;
  document.getElementById('complaintSearch').addEventListener('input', function () {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(render, 250);
  });
  document.getElementById('statusFilter').addEventListener('change', render);
  document.getElementById('priorityFilter').addEventListener('change', render);

  await render();
});