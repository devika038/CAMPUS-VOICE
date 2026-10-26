document.addEventListener('DOMContentLoaded', async function () {
  const app = window.CampusVoice;
  const user = app.checkPageRole('student');
  if (!user) return;

  app.wireCommon();
  document.getElementById('studentGreeting').textContent = 'Hello, ' + user.name;
  document.getElementById('studentRole').textContent = user.role;

  const categorySelect = document.getElementById('complaintCategory');
  const departmentSelect = document.getElementById('complaintDepartment');
  await Promise.all([
    app.populateCategories(categorySelect, 'Select a category'),
    app.populateDepartments(departmentSelect, 'Select a department')
  ]);

  const body = document.getElementById('myComplaintsBody');
  const empty = document.getElementById('studentEmpty');
  let loadedComplaints = [];

  async function render() {
    try {
      loadedComplaints = await app.apiFetch('/complaints/my');
      if (!Array.isArray(loadedComplaints)) loadedComplaints = [];
    } catch (err) {
      console.error('Failed to load my complaints:', err);
      loadedComplaints = [];
    }

    document.getElementById('myComplaintCount').textContent = loadedComplaints.length;
    empty.hidden = loadedComplaints.length > 0;

    body.innerHTML = loadedComplaints.map(function (complaint) {
      return '<tr>' +
        '<td class="id-cell">' + app.escapeText(complaint.id) + '</td>' +
        '<td class="title-cell">' + app.escapeText(complaint.title) + '</td>' +
        '<td>' + app.escapeText(complaint.category) + '</td>' +
        '<td>' + app.escapeText(app.formatDate(complaint.createdAt)) + '</td>' +
        '<td><span class="priority-badge priority-' + app.escapeText(complaint.priority.toLowerCase()) + '">' + app.escapeText(complaint.priority) + '</span></td>' +
        '<td><span class="status-badge status-' + app.statusClass(complaint.status) + '">' + app.escapeText(complaint.status.replace('_', ' ')) + '</span></td>' +
        '<td><button class="text-button" type="button" data-view="' + app.escapeText(complaint.id) + '">View Details</button></td>' +
        '</tr>';
    }).join('');

    body.querySelectorAll('[data-view]').forEach(function (button) {
      button.addEventListener('click', function () { showDetails(button.dataset.view); });
    });
  }

  async function showDetails(id) {
    let complaint = loadedComplaints.find(function (item) { return item.id === id; });
    try {
      const fresh = await app.apiFetch('/complaints/' + encodeURIComponent(id));
      if (fresh) complaint = fresh;
    } catch (e) {
      // Use cached if fetch fails
    }
    if (!complaint) return;

    document.getElementById('detailTitle').textContent = complaint.title;
    document.getElementById('detailContent').innerHTML =
      '<div class="detail-meta">' +
      '<span class="id-cell">' + app.escapeText(complaint.id) + '</span>' +
      '<span class="priority-badge priority-' + complaint.priority.toLowerCase() + '">' + app.escapeText(complaint.priority) + '</span>' +
      '<span class="status-badge status-' + app.statusClass(complaint.status) + '">' + app.escapeText(complaint.status.replace('_', ' ')) + '</span>' +
      '</div>' +
      '<dl class="detail-grid">' +
      '<div><dt>Category</dt><dd>' + app.escapeText(complaint.category) + '</dd></div>' +
      '<div><dt>Department</dt><dd>' + app.escapeText(complaint.department) + '</dd></div>' +
      '<div><dt>Submitted</dt><dd>' + app.escapeText(new Date(complaint.createdAt).toLocaleString()) + '</dd></div>' +
      '<div><dt>Submitted by</dt><dd>' + app.escapeText(complaint.ownerName || user.name) + '</dd></div>' +
      '</dl>' +
      '<div class="detail-description"><h3>Description</h3><p>' + app.escapeText(complaint.description).replace(/\n/g, '<br>') + '</p></div>' +
      '<h3 class="timeline-heading">Status timeline</h3>' + app.timelineMarkup(complaint.audit);

    document.getElementById('complaintDetails').showModal();
  }

  const form = document.getElementById('complaintForm');
  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    const error = document.getElementById('complaintError');
    app.showError(error, '');

    const title = document.getElementById('complaintTitle').value.trim();
    const category = categorySelect.value;
    const department = departmentSelect.value;
    const description = document.getElementById('complaintDescription').value.trim();

    if (!title || !category || !department || !description) {
      return app.showError(error, 'Complete each field before submitting.');
    }

    try {
      await app.apiFetch('/complaints', {
        method: 'POST',
        body: { title, category, department, description }
      });
      form.reset();
      await render();
    } catch (err) {
      app.showError(error, err.message || 'Failed to submit complaint.');
    }
  });

  await render();
});