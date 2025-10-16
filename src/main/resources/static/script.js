// The API_BASE variable (e.g., "http://13.200.137.176") is set in index.html by Jenkins.
// The actual endpoint is used consistently below.
const API_ENDPOINT = `${API_BASE}/api/tasks`; // This is the corrected, central resource URL

async function loadTasks() {
    // 🚨 FIX: Using the corrected API_ENDPOINT constant
    const res = await fetch(API_ENDPOINT);
    
    // The previous 404 caused this TypeError, which will be fixed once the URL is correct.
    const tasks = await res.json();
    const list = document.getElementById("taskList");
    list.innerHTML = "";
    tasks.forEach(task => {
        const li = document.createElement("li");
        li.innerHTML = `
            <input type="checkbox" ${task.completed ? "checked" : ""} onclick="toggleTask(${task.id}, ${!task.completed})">
            ${task.title}
            <button onclick="deleteTask(${task.id})">Delete</button>
        `;
        list.appendChild(li);
    });
}

async function addTask() {
    const title = document.getElementById("taskInput").value;
    if (!title) return;

    // 🚨 FIX: Using the corrected API_ENDPOINT constant
    await fetch(API_ENDPOINT, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, completed: false })
    });

    document.getElementById("taskInput").value = "";
    loadTasks();
}

async function toggleTask(id, completed) {
    // 🚨 FIX: Using the corrected API_ENDPOINT constant
    await fetch(`${API_ENDPOINT}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: "", completed })
    });
    loadTasks();
}

async function deleteTask(id) {
    // 🚨 FIX: Using the corrected API_ENDPOINT constant
    await fetch(`${API_ENDPOINT}/${id}`, {
        method: "DELETE"
    });
    loadTasks();
}

loadTasks();