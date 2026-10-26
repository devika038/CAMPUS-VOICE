(function () {
  const departments = ['Computer Science', 'Electrical', 'Mechanical', 'Civil', 'Hostel', 'Maintenance', 'Library', 'Administration'];
  const categories = ['Infrastructure', 'Academics', 'Hostel', 'Harassment', 'Lab Equipment', 'Library', 'Examination', 'Other'];
  
  const tokenKey = 'campusVoiceToken';
  const userKey = 'campusVoiceUser';

  const API_BASE = (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080')
    ? 'http://localhost:8080/api'
    : '/api');

  function getToken() {
    return sessionStorage.getItem(tokenKey);
  }

  function getSession() {
    try {
      return JSON.parse(sessionStorage.getItem(userKey) || 'null');
    } catch (error) {
      return null;
    }
  }

  function setSession(token, user) {
    if (token) sessionStorage.setItem(tokenKey, token);
    if (user) sessionStorage.setItem(userKey, JSON.stringify(user));
  }

  function clearSession() {
    sessionStorage.removeItem(tokenKey);
    sessionStorage.removeItem(userKey);
  }

  async function apiFetch(endpoint, options) {
    options = options || {};
    const headers = options.headers || {};
    const token = getToken();
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }
    options.headers = headers;

    const url = endpoint.startsWith('http') ? endpoint : API_BASE + endpoint;
    const response = await fetch(url, options);

    let data = null;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json().catch(() => null);
    }

    if (!response.ok) {
      const message = (data && data.message) ? data.message : 'Request failed with status ' + response.status;
      const error = new Error(message);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  }

  function escapeText(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, function (character) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character];
    });
  }

  function fillOptions(select, values, firstLabel) {
    if (!select) return;
    select.innerHTML = '<option value="">' + escapeText(firstLabel) + '</option>' + values.map(function (value) {
      const label = typeof value === 'object' ? value.name : value;
      return '<option value="' + escapeText(label) + '">' + escapeText(label) + '</option>';
    }).join('');
  }

  async function populateDepartments(selectElement, firstLabel) {
    if (!selectElement) return;
    try {
      const depts = await apiFetch('/departments');
      if (Array.isArray(depts) && depts.length) {
        fillOptions(selectElement, depts.map(d => d.name), firstLabel);
        return;
      }
    } catch (e) {
      // Fall back to predefined defaults
    }
    fillOptions(selectElement, departments, firstLabel);
  }

  async function populateCategories(selectElement, firstLabel) {
    if (!selectElement) return;
    try {
      const cats = await apiFetch('/categories');
      if (Array.isArray(cats) && cats.length) {
        fillOptions(selectElement, cats.map(c => c.name), firstLabel);
        return;
      }
    } catch (e) {
      // Fall back to predefined defaults
    }
    fillOptions(selectElement, categories, firstLabel);
  }

  function redirectFor(user) {
    location.href = user.role === 'Department Administrator' ? 'admin.html' : 'student.html';
  }

  function checkPageRole(allowedRole) {
    const user = getSession();
    const token = getToken();
    if (!user || !token || 
        (allowedRole === 'student' && !['Student', 'Faculty'].includes(user.role)) || 
        (allowedRole === 'admin' && user.role !== 'Department Administrator')) {
      clearSession();
      location.replace('index.html');
      return null;
    }
    return user;
  }

  async function logout() {
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch (ignored) {}
    clearSession();
    location.replace('index.html');
  }

  function showError(element, message) {
    if (element) element.textContent = message || '';
  }

  function formatDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  function nextPriority(category, description) {
    const base = { Harassment: 3, Infrastructure: 2, 'Lab Equipment': 2, Hostel: 1, Examination: 1, Academics: 1, Library: 0, Other: 0 }[category] || 0;
    const urgent = /\b(urgent|danger|fire|unsafe)\b/i.test(description);
    return ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'][Math.min(3, base + (urgent ? 1 : 0))];
  }

  function statusClass(status) { return String(status || '').toLowerCase().replace('_', '-'); }

  function timelineMarkup(entries) {
    if (!entries || !entries.length) return '<p class="muted">No status history recorded.</p>';
    return '<ol class="timeline">' + entries.map(function (entry) {
      const transition = entry.oldStatus ? escapeText(entry.oldStatus) + ' <span>→</span> ' + escapeText(entry.newStatus) : escapeText(entry.newStatus);
      return '<li><span class="timeline-dot"></span><div><strong>' + transition + '</strong><time>' + escapeText(new Date(entry.time).toLocaleString()) + '</time><p>' + escapeText(entry.remarks || 'No remarks') + '</p><small>' + escapeText(entry.who || 'Campus Voice') + '</small></div></li>';
    }).join('') + '</ol>';
  }

  function wireCommon() {
    document.querySelectorAll('[data-logout]').forEach(function (button) { button.addEventListener('click', logout); });
    document.querySelectorAll('[data-close-dialog]').forEach(function (button) {
      button.addEventListener('click', function () { button.closest('dialog').close(); });
    });
    document.querySelectorAll('dialog.modal').forEach(function (dialog) {
      dialog.addEventListener('click', function (event) { if (event.target === dialog) dialog.close(); });
    });
  }

  window.CampusVoice = {
    departments,
    categories,
    escapeText,
    fillOptions,
    populateDepartments,
    populateCategories,
    getSession,
    setSession,
    clearSession,
    apiFetch,
    redirectFor,
    checkPageRole,
    logout,
    showError,
    formatDate,
    nextPriority,
    statusClass,
    timelineMarkup,
    wireCommon
  };

  document.addEventListener('DOMContentLoaded', function () {
    const signupDepartment = document.getElementById('signupDepartment');
    populateDepartments(signupDepartment, 'Select a department');

    const signupRole = document.getElementById('signupRole');
    const departmentField = document.getElementById('signupDepartmentField');
    if (signupRole) signupRole.addEventListener('change', function () {
      const isAdmin = signupRole.value === 'Department Administrator';
      departmentField.hidden = !isAdmin;
      signupDepartment.required = isAdmin;
      if (!isAdmin) signupDepartment.value = '';
    });

    const loginForm = document.getElementById('loginForm');
    const signupForm = document.getElementById('signupForm');
    if (!loginForm || !signupForm) return;

    const loginTab = document.getElementById('loginTab');
    const signupTab = document.getElementById('signupTab');
    function selectTab(which) {
      const loginSelected = which === 'login';
      loginTab.classList.toggle('active', loginSelected);
      signupTab.classList.toggle('active', !loginSelected);
      loginTab.setAttribute('aria-selected', String(loginSelected));
      signupTab.setAttribute('aria-selected', String(!loginSelected));
      document.getElementById('loginPanel').hidden = !loginSelected;
      document.getElementById('signupPanel').hidden = loginSelected;
    }
    loginTab.addEventListener('click', function () { selectTab('login'); });
    signupTab.addEventListener('click', function () { selectTab('signup'); });

    loginForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      const error = document.getElementById('loginError');
      showError(error, '');
      const email = document.getElementById('loginEmail').value.trim().toLowerCase();
      const password = document.getElementById('loginPassword').value;
      const role = document.getElementById('loginRole').value;

      if (!email || !password || !role) {
        return showError(error, 'Please fill in all login fields.');
      }

      try {
        const response = await apiFetch('/auth/login', {
          method: 'POST',
          body: { email, password, role }
        });
        setSession(response.token, response.user);
        redirectFor(response.user);
      } catch (err) {
        showError(error, err.message || 'Login failed.');
      }
    });

    signupForm.addEventListener('submit', async function (event) {
      event.preventDefault();
      const error = document.getElementById('signupError');
      showError(error, '');
      const name = document.getElementById('signupName').value.trim();
      const email = document.getElementById('signupEmail').value.trim().toLowerCase();
      const password = document.getElementById('signupPassword').value;
      const confirm = document.getElementById('confirmPassword').value;
      const role = signupRole.value;
      const department = signupDepartment.value;

      if (!name || !email || !role) return showError(error, 'Complete all required fields.');
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return showError(error, 'Enter a valid email address.');
      if (password.length < 8) return showError(error, 'Password must be at least 8 characters.');
      if (password !== confirm) return showError(error, 'The passwords do not match.');
      if (role === 'Department Administrator') {
        return showError(error, 'Department Administrator accounts are provisioned by system administration and cannot be created via public sign-up.');
      }

      try {
        const response = await apiFetch('/auth/signup', {
          method: 'POST',
          body: { name, email, password, confirmPassword: confirm, role, department }
        });
        setSession(response.token, response.user);
        redirectFor(response.user);
      } catch (err) {
        showError(error, err.message || 'Registration failed.');
      }
    });

    const existingUser = getSession();
    if (existingUser && getToken()) {
      redirectFor(existingUser);
    }
  });
}());